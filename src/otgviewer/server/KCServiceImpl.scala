package otgviewer.server

import com.google.gwt.user.server.rpc.RemoteServiceServlet
import otgviewer.client.KCService
import kyotocabinet.DB
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.OTGQueries
import java.util.{ List => JList, ArrayList }
import otgviewer.shared.DataFilter
import otgviewer.shared.Synthetic
import otgviewer.shared.ValueType
import otgviewer.shared.Group
import otgviewer.shared.BarcodeColumn
import otgviewer.shared.Series
import otg.ExprValue
import otgviewer.shared.Barcode
import javax.servlet.http.HttpSession
import otg.sparql.AffyProbes
import otg.CSVHelper
import otg.OTGSeriesQuery
import otg.sparql._
import otg.OTGCabinet
import bioweb.shared.array.ExpressionRow
import bioweb.shared.array.ExpressionValue
import bioweb.server.array.ArrayServiceImpl
import friedrich.data.immutable.VVector


/**
 * This servlet is responsible for obtaining and manipulating data from the Kyoto
 * Cabinet databases.
 */
class KCServiceImpl extends ArrayServiceImpl[Barcode, DataFilter] with KCService {
  import Conversions._
  import scala.collection.JavaConversions._
  import UtilsS._

  private var foldsDB: DB = _
  private var absDB: DB = _
  private var tgConfig: Configuration = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))    
  }
  
  // Useful for testing
  def localInit(config: Configuration) {
    val homePath = config.toxygatesHomeDir
    foldsDB = OTGQueries.open(homePath + "/otgf.kct")
    absDB = OTGQueries.open(homePath + "/otg.kct")
    otg.Configuration.owlimRepositoryName = config.owlimRepositoryName
    otg.Configuration.otgHomeDir = homePath
    // TODO: set csv parameters too
    println("KC databases are open")
  }

  override def destroy() {
    println("Closing KC databases")
    foldsDB.close()
    absDB.close()
    super.destroy()
  }

  //TODO lift up
  protected def getSessionData() = new SessionData(getThreadLocalRequest().getSession())

  protected class SessionData(val session: HttpSession) {
    def ungroupedUnfiltered: ExprMatrix = session.getAttribute("ungroupedUnfiltered").asInstanceOf[ExprMatrix]
    def ungroupedUnfiltered_=(v: ExprMatrix) = session.setAttribute("ungroupedUnfiltered", v)

    def ungroupedFiltered: ExprMatrix = session.getAttribute("ungroupedFiltered").asInstanceOf[ExprMatrix]
    def ungroupedFiltered_=(v: ExprMatrix) = session.setAttribute("ungroupedFiltered", v)

    def rendered: ExprMatrix = session.getAttribute("groupedFiltered").asInstanceOf[ExprMatrix]
    def rendered_=(v: ExprMatrix) = session.setAttribute("groupedFiltered", v)

    // Like rendered but without synthetic columns
    def noSynthetics: ExprMatrix = session.getAttribute("noSynthetics").asInstanceOf[ExprMatrix]
    def noSynthetics_=(v: ExprMatrix) = session.setAttribute("noSynthetics", v)
    
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

  private def filterProbes(filter: DataFilter, probes: Seq[String]): Seq[String] =
    if (probes == null || probes.size == 0) {
      OTGQueries.probeIds(filter).toSeq
    } else {
      OTGQueries.filterProbes(probes, filter)
    }

  private def getDB(typ: ValueType) = typ match {
    case ValueType.Folds    => foldsDB
    case ValueType.Absolute => absDB
  }

  private def getExprValues(filter: DataFilter, barcodes: Seq[String], probes: Seq[String],
                            typ: ValueType, sparseRead: Boolean): ExprMatrix = {
    val db = getDB(typ)
    val sorted = OTGQueries.sortSamples(barcodes.map(otg.Sample(_)))
    val data = OTGQueries.presentValuesByBarcodesAndProbes(db, sorted, probes, sparseRead, filter)
    val jdata = data.map(r => new VVector(r.map(asJava(_))))
    new ExprMatrix(jdata, jdata.size, jdata(0).size,
        Map() ++ probes.zipWithIndex, //rows
        Map() ++ sorted.map(_.code).zipWithIndex, //columns
        probes.map(new RowAnnotation(_, null, null, null)).toVector)    
  }


  def loadDataset(filter: DataFilter, columns: JList[BarcodeColumn], probes: Array[String],
                  typ: ValueType, absValFilter: Double, syntheticColumns: JList[Synthetic]): Int = {

    def barcodes(columns: Seq[BarcodeColumn]): Seq[String] = {
      columns.flatMap(_ match {
        case g: Group   => g.getSamples
        case b: Barcode => Vector(b)
        case _          => Vector()
      }).map(_.id)
    }

    val filtered = filterProbes(filter, null).toSeq
    val session = getSessionData()
    
    //load with all probes for this filter
    val data = getExprValues(filter, barcodes(columns.toVector), filtered, typ, false)

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

    val session = getSessionData()
    val data = session.ungroupedUnfiltered

    val groupedData = makeGroups(data, columns)

    val filteredProbes = filterProbes(filter, probes)

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
          groupedFiltered.asRows.drop(offset).take(size), 
          session.params.filter)
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
  private def insertAnnotations(rows: Seq[ExpressionRow], f: DataFilter): Seq[ExpressionRow] = {
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
    
    val realProbes = filterProbes(filter, probes)
    val r = getExprValues(filter, sbc, realProbes, typ, sparseRead)
    
    //When we have obtained the data in r, it might no longer be sorted in the order that the user
    //requested. Thus we use selectNamedRows here to force the sort order they wanted.
    
    val raw = r.selectNamedColumns(sbc).asRows
    val rows = if (withSymbols) {
      insertAnnotations(raw, filter)
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
      CSVHelper.writeCSV(rowNames, colNames, geneIds, session.rendered.data.map(_.map(asScala(_))))
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