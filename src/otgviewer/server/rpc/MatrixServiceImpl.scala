package otgviewer.server.rpc

import java.util.ArrayList
import java.util.{List => JList}
import java.util.{List => JList}

import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList

import Conversions.asJava
import Conversions.asScala
import Conversions.speciesFromFilter
import bioweb.server.array.ArrayServiceImpl
import bioweb.shared.array.ExpressionRow
import bioweb.shared.array.ExpressionValue
import friedrich.data.immutable.VVector
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.ServletException
import javax.servlet.http.HttpSession
import otg.CSVHelper
import otg.ExprValue
import otg.ExprValue
import otg.OTGContext
import otg.db.MicroarrayDBReader
import otg.sparql._
import otg.sparql.AffyProbes
import otg.sparql.AffyProbes
import otg.sparql.BioObjects.makeRich
import otg.sparql.Probe
import otgviewer.client.rpc.MatrixService
import otgviewer.server.Configuration
import otgviewer.server.ExprMatrix
import otgviewer.server.ExpressionValueReader
import otgviewer.server.RowAnnotation
import otgviewer.server.UtilsS
import otgviewer.shared.Barcode
import otgviewer.shared.BarcodeColumn
import otgviewer.shared.DataFilter
import otgviewer.shared.Group
import otgviewer.shared.Synthetic
import otgviewer.shared.ValueType

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 */
class MatrixServiceImpl extends ArrayServiceImpl[Barcode, DataFilter] with MatrixService {
  import Conversions._
  import scala.collection.JavaConversions._
  import UtilsS._

  private var foldsDBReader: ExpressionValueReader[_] = _

  private var absDBReader: ExpressionValueReader[_] = _
  
  private var tgConfig: Configuration = _
  private var csvDirectory: String = _
  private var csvUrlBase: String = _
  private implicit var context: OTGContext = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))    
  }

  // Useful for testing
  def localInit(config: Configuration) {    
    csvDirectory = config.csvDirectory
    csvUrlBase = config.csvUrlBase
    context = config.context
    // Future: construct DB in context
    foldsDBReader = config.foldsDBReader        
    absDBReader = config.absoluteDBReader

    OwlimLocalRDF.setContextForAll(context)
    println("Microarray databases are open")
  }

  override def destroy() {
    println("Closing KC databases")
    foldsDBReader.close()
    absDBReader.close()
    super.destroy()
  }

  //TODO lift up
  protected def getSessionData() = new SessionData(getThreadLocalRequest().getSession())

  protected class SessionData(val session: HttpSession) {
    private[this] def readMatrix(name: String): ExprMatrix =
      session.getAttribute(name).asInstanceOf[ExprMatrix]
    private[this] def writeMatrix(name: String, v: ExprMatrix): Unit =
      session.setAttribute(name, v)
    
    def ungroupedUnfiltered: ExprMatrix = readMatrix("ungroupedUnfiltered")
    def ungroupedUnfiltered_=(v: ExprMatrix) = writeMatrix("ungroupedUnfiltered", v)

    def ungroupedFiltered: ExprMatrix = readMatrix("ungroupedFiltered")
    def ungroupedFiltered_=(v: ExprMatrix) = writeMatrix("ungroupedFiltered", v)

    def rendered: ExprMatrix = readMatrix("groupedFiltered")
    def rendered_=(v: ExprMatrix) = writeMatrix("groupedFiltered", v)

    // Like rendered but without synthetic columns
    def noSynthetics: ExprMatrix = readMatrix("noSynthetics")
    def noSynthetics_=(v: ExprMatrix) = writeMatrix("noSynthetics", v)
    
    def params: DataViewParams = {
      val r = session.getAttribute("params").asInstanceOf[DataViewParams]
      if (r != null) {
        r
      } else {
        val p = new DataViewParams()
        this.params = p
        p
      }
    }
    def params_=(v: DataViewParams) = session.setAttribute("params", v)
  }

  //Should this be in owlimService?
  def identifiersToProbes(filter: DataFilter, identifiers: Array[String], precise: Boolean): Array[String] =
    AffyProbes.identifiersToProbes(filter, identifiers, precise).map(_.identifier).toArray

  private def filterProbes(probes: Seq[String])(implicit filter: DataFilter): Seq[String] = {
    val pmap = context.probes(filter)
    if (probes == null || probes.size == 0) {
      pmap.tokens.toSeq
    } else {
      probes.filter(pmap.isToken)      
    }
  }

  private def getDBReader(typ: ValueType) = typ match {
    case ValueType.Folds    => foldsDBReader
    case ValueType.Absolute => absDBReader
  }

  private def getExprValues(barcodes: Seq[String], probes: Seq[String],
                            typ: ValueType, sparseRead: Boolean)(implicit filter: DataFilter): ExprMatrix = {
    val dbr = getDBReader(typ)
    val pmap = context.probes(filter)
    val sorted = dbr.db.sortSamples(barcodes.map(otg.Sample(_)))
    val data = dbr.presentValuesForSamplesAndProbes(filter, sorted, 
        probes.map(pmap.pack), sparseRead)
    new ExprMatrix(data.map(new VVector(_)), data.size, data(0).size,
        Map() ++ probes.zipWithIndex, //rows
        Map() ++ sorted.map(_.sampleId).zipWithIndex, //columns
        probes.map(new RowAnnotation(_, null, null, null)).toVector)    
  }


  def loadDataset(filter: DataFilter, columns: JList[BarcodeColumn], probes: Array[String],
                  typ: ValueType, absValFilter: Double, 
                  syntheticColumns: JList[Synthetic]): Int = {

    implicit val f = filter
    
    def barcodes(columns: Seq[BarcodeColumn]): Seq[String] = {
      columns.flatMap(_ match {
        case g: Group   => g.getSamples
        case b: Barcode => Vector(b)
        case _          => Vector()
      }).map(_.id)
    }

    val filtered = filterProbes(null).toSeq
    val session = getSessionData()
    
    //load with all probes for this filter
    val data = getExprValues(barcodes(columns.toVector), filtered, typ, false)

    session.ungroupedUnfiltered = data
    refilterData(filter, columns, probes, absValFilter, syntheticColumns)
  }

  private def presentMean(vs: Seq[ExpressionValue]): ExpressionValue = {
    asJava(ExprValue.presentMean(vs.map(asScala(_)), "")) //TODO: simplify
  }
  
  private def makeGroups(data: ExprMatrix, columns: Seq[BarcodeColumn]) = {    
    val groupedColumns = columns.map(_ match {
      case g: Group => {
       (0 until data.rows).map(r => {
          val vs = g.getSamples.map(bc => data.obtainColumn(bc.getCode)).map(data(r, _))
          presentMean(vs)
        })
      }
      case b: Barcode => data.column(data.obtainColumn(b.getCode))
    })
         
    val alloc = Map() ++ columns.map(_.toString).zipWithIndex
    data.copyWithColumns(groupedColumns).copyWithColAlloc(alloc)
  }
  
  def refilterData(filter: DataFilter, columns: JList[BarcodeColumn], probes: Array[String], absValFilter: Double,
                   syntheticColumns: JList[Synthetic]): Int = {
    println("Refilter probes: " + probes)
    if (probes != null) {
      println("Length: " + probes.size)
    }

    implicit val fl = filter
    val session = getSessionData()
    val data = session.ungroupedUnfiltered
    val groupedData = makeGroups(data, columns)
    val filteredProbes = filterProbes(probes)

    //filter by abs. value
    def f(r: Seq[ExpressionValue], before: Int): Boolean = r.take(before).exists(v =>
      (Math.abs(v.value) >= absValFilter - 0.0001) || (java.lang.Double.isNaN(v.value) && absValFilter == 0))

    //Pick out rows that correspond to the selected probes only, and filter them by absolute value
    val (ngfd, nfd) = groupedData.modifyJointly(data, 
        _.selectNamedRows(filteredProbes.toSeq).filterRows(r => f(r, groupedData.columns)))

    session.rendered = ngfd
    session.noSynthetics = ngfd
    session.ungroupedFiltered = nfd 
    
    if (ngfd.rows > 0) {
      println("Stored " + ngfd.rows + " x " + ngfd.columns + " items in session")
    } else {
      System.out.println("Stored empty data in session");
    }

    val params = session.params
    params.mustSort = true
    params.filter = filter

    syntheticColumns.foreach(t => addTwoGroupTest(t.asInstanceOf[Synthetic.TwoGroupSynthetic]))

    ngfd.rows
  }

  def datasetItems(offset: Int, size: Int, sortColumn: Int, ascending: Boolean): JList[ExpressionRow] = {
    def sortData(v1: Seq[ExpressionValue],
                 v2: Seq[ExpressionValue]): Boolean = {
      val ev1 = v1(sortColumn)
      val ev2 = v2(sortColumn)
      if (ev1.call == 'A' && ev2.call != 'A') {
        false
      } else if (ev1.call != 'A' && ev2.call == 'A') {
        true
      } else {
        if (ascending) { ev1.value < ev2.value } else { ev1.value > ev2.value }
      }
    }

    println("SortCol: " + sortColumn + " asc: " + ascending)

    val session = getSessionData()
    var groupedFiltered = session.rendered
    val params = session.params
    if (groupedFiltered != null) {
      println("I had " + groupedFiltered.rows + " rows stored")

      //At this point sorting may happen		
      if (sortColumn > -1 && (sortColumn != params.sortColumn ||
        ascending != params.sortAsc || params.mustSort)) {
        //OK, we need to re-sort it and then re-store it
        params.sortColumn = sortColumn
        params.sortAsc = ascending
        params.mustSort = false

        //sort both the "ungrouped filtered" and the "no synthetics" along with the "rendered"
        val (grf, ugrf) = groupedFiltered.modifyJointly(session.ungroupedFiltered,
          _.sortRows(sortData))
        val (_, nosyn) = groupedFiltered.modifyJointly(session.noSynthetics,
          _.sortRows(sortData))

        groupedFiltered = grf
        session.rendered = grf
        session.noSynthetics = nosyn
        session.ungroupedFiltered = ugrf
      }
      new ArrayList[ExpressionRow](insertAnnotations(
          groupedFiltered.asRows.drop(offset).take(size))(session.params.filter)
          )
    } else {
      new ArrayList[ExpressionRow]()
    }
  }

  /**
   * Dynamically obtain annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  private def insertAnnotations(rows: Seq[ExpressionRow])(implicit f: DataFilter): Seq[ExpressionRow] = {
    val probes = rows.map(r => Probe(r.getProbe))
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val attribs = c.withAttributes(probes, f)
      val pm = Map() ++ attribs.map(a => (a.identifier -> a))
      
      rows.map(or => {
        if (!pm.containsKey(or.getProbe)) {
          println("missing key: " + or.getProbe)
        }
        val p = pm(or.getProbe)
        new ExpressionRow(p.identifier, p.name, p.genes.map(_.identifier).toArray,
            p.symbols.map(_.symbol).toArray, or.getValues)
      })      
    })
  }
  
  def getFullData(filter: DataFilter, barcodes: JList[String], probes: Array[String],
                  typ: ValueType, sparseRead: Boolean, withSymbols: Boolean): JList[ExpressionRow] = {
    val sbc = barcodes.toSeq
    implicit val f = filter
    
    val realProbes = filterProbes(probes)
    val r = getExprValues(sbc, realProbes, typ, sparseRead)
    
    //When we have obtained the data in r, it might no longer be sorted in the order that the user
    //requested. Thus we use selectNamedRows here to force the sort order they wanted.
    
    val raw = r.selectNamedColumns(sbc).asRows
    val rows = if (withSymbols) {
      insertAnnotations(raw)
    } else {
      raw
    }
    new ArrayList[ExpressionRow](rows)
  }

  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): Unit = {
    val session = getSessionData()
    val rendered = session.rendered
    val data = session.ungroupedFiltered
    val g1 = test.getGroup1
    val g2 = test.getGroup2
    val withTest = test match {
      case ut: Synthetic.UTest => {
        rendered.appendUTest(data, g1.getSamples.map(_.getCode),
          g2.getSamples.map(_.getCode), ut.getShortTitle)
      }
      case tt: Synthetic.TTest => {
        rendered.appendTTest(data, g1.getSamples.map(_.getCode),
          g2.getSamples.map(_.getCode), tt.getShortTitle)
      }
      case md: Synthetic.MeanDifference => {
        rendered.appendDiffTest(data, g1.getSamples.map(_.getCode),
            g2.getSamples.map(_.getCode), md.getShortTitle)
      }
    }
    session.rendered = withTest
  }
  
  def removeTwoGroupTests(): Unit = {
    val session = getSessionData()
    // This is the only reason why we keep the noSynthetics around
    session.rendered = session.noSynthetics    
  }

  def prepareCSVDownload(): String = {
    import BioObjects._
    
    val session = getSessionData()
    val rendered = session.rendered
    if (rendered != null) {
      println("I had " + session.rendered.rows + " rows stored")
    }
    
    val colNames = rendered.sortedColumnMap.map(_._1)
    val rowNames = rendered.sortedRowMap.map(_._1)
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val gis = c.allGeneIds(session.params.filter).mapMValues(_.identifier)      
      val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Seq.empty)).map(_.mkString(" "))
      CSVHelper.writeCSV(csvDirectory, csvUrlBase, rowNames, colNames, geneIds, session.rendered.data.map(_.map(asScala(_))))
    })
  }

  def getGenes(limit: Int): Array[String] = {
    val session = getSessionData()
    val rendered = session.rendered
    var rowNames = rendered.sortedRowMap.map(_._1)
    println(rowNames.take(10))
    if (limit != -1) {
      rowNames = rowNames.take(limit)
    }
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val gis = c.allGeneIds(session.params.filter)
//      println(rowNames.take(10))      
      val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
//      println(geneIds.take(10))
      geneIds.flatten.map(_.identifier).toArray
    })
  }



}