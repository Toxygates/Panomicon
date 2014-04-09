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
import otgviewer.server.RowAnnotation
import otgviewer.server.UtilsS
import otgviewer.shared.Barcode
import otgviewer.shared.BarcodeColumn
import otgviewer.shared.DataFilter
import otgviewer.shared.Group
import otgviewer.shared.Synthetic
import otgviewer.shared.ValueType
import otgviewer.server.ManagedMatrix
import otg.db.kyotocabinet.KCExtMicroarrayDB
import otgviewer.server.ExtFoldValueMatrix
import otgviewer.server.NormalizedIntensityMatrix
import otg.db.kyotocabinet.KCMicroarrayDB
import otgviewer.server.FoldValueMatrix
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.server.ApplicationClass
import otgviewer.shared.StringList
import org.intermine.webservice.client.core.ServiceFactory
import org.intermine.webservice.client.services.ListService
import otgviewer.server.TargetMine
import otgviewer.server.Feedback

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 */
class MatrixServiceImpl extends ArrayServiceImpl[Barcode, DataFilter] with MatrixService {
  import Conversions._
  import scala.collection.JavaConversions._
  import UtilsS._

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
    tgConfig = config
    
    OwlimLocalRDF.setContextForAll(context)
    OTGSamples.connect
    AffyProbes.connect
    println("Microarray databases are open")
  }

  override def destroy() {
    println("Closing KC databases")
    context.closeReaders
    super.destroy()
  }
  
  def getSessionData(): ManagedMatrix[_] = 
    getThreadLocalRequest().getSession().getAttribute("matrix").asInstanceOf[ManagedMatrix[_]]
  
  def setSessionData(m: ManagedMatrix[_]) =
    getThreadLocalRequest().getSession().setAttribute("matrix", m)

  //Should this be in sparqlService?
  def identifiersToProbes(filter: DataFilter, identifiers: Array[String], precise: Boolean): Array[String] =
    AffyProbes.identifiersToProbes(filter, identifiers, precise).map(_.identifier).toArray

    /**
     * Filter probes for one species.
     */
  private def filterProbes(probes: Seq[String])(implicit filter: DataFilter): Seq[String] = {
    val pmap = context.probes(filter)
    if (probes == null || probes.size == 0) {
      pmap.tokens.toSeq
    } else {
      probes.filter(pmap.isToken)      
    }
  }

  /**
   * Filter probes for all species.
   */
  private def filterProbesAllSpecies(probes: Seq[String]): Seq[String] = {
    val pmaps = otg.Species.values.toList.map(context.probes(_))
    if (probes == null || probes.size == 0) {
      throw new Exception("Requesting all probes for all species is not permitted.")
    } else {
      probes.filter(p => pmaps.exists(m => m.isToken(p)))
    }
  }
  
  private[this] def makeMatrix(requestColumns: Seq[Group],
    initProbes: Array[String], typ: ValueType, sparseRead: Boolean = false)
  (implicit filter: DataFilter): ManagedMatrix[_] = {
    val reader = if (typ == ValueType.Absolute) {
      context.absoluteDBReader
    } else {
      if (tgConfig.foldsDBVersion == 2) {
        context.foldsDBReaderV2
      } else {
        context.foldsDBReaderV1
      }
    }
    
    val enhancedCols = tgConfig.applicationClass == ApplicationClass.Adjuvant

    reader match {
      case ext: KCExtMicroarrayDB =>
        assert(typ == ValueType.Folds)
        new ExtFoldValueMatrix(requestColumns, ext, initProbes, sparseRead, enhancedCols)
      case db: KCMicroarrayDB =>
        if (typ == ValueType.Absolute) {
          new NormalizedIntensityMatrix(requestColumns, db, initProbes, sparseRead, enhancedCols)
        } else {
          new FoldValueMatrix(requestColumns, db, initProbes, sparseRead)
        }
      case _ => throw new Exception("Unexpected DB reader type")
    }   
  }

  def loadDataset(filter: DataFilter, groups: JList[Group], probes: Array[String],
                  typ: ValueType, syntheticColumns: JList[Synthetic]): ManagedMatrixInfo = {
    implicit val f = filter    
    val allProbes = filterProbes(null).toArray
    val mm = makeMatrix(groups.toVector, allProbes, typ)    
    setSessionData(mm)
    selectProbes(probes)
  }

  def selectProbes(probes: Array[String]): ManagedMatrixInfo = {
    if (probes != null) {
      println("Refilter probes: " + probes.length)      
    }
    val mm = getSessionData
    
//    mm.filterData(Some(absValFilter))	
    if (probes != null && probes.length > 0) {
    	mm.selectProbes(probes)
    } else {
      implicit val f = mm.filter
        val allProbes = filterProbes(null).toArray
        mm.selectProbes(allProbes)
    }
    mm.info
  }
  
  def setColumnThreshold(column: Int, threshold: java.lang.Double): ManagedMatrixInfo = {
    val mm = getSessionData 
    println(s"Filter column $column at $threshold")
    mm.setFilterThreshold(column, threshold)
    mm.info
  }

  def datasetItems(offset: Int, size: Int, sortColumn: Int,
    ascending: Boolean): JList[ExpressionRow] = {

    println("SortCol: " + sortColumn + " asc: " + ascending)
    val session = getSessionData()
    if (sortColumn != session.sortColumn || ascending != session.sortAscending) {
      session.sort(sortColumn, ascending)
    }
    val mm = session.current
    new ArrayList[ExpressionRow](
      insertAnnotations(mm.asRows.drop(offset).take(size))(session.filter))
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
    
    val realProbes = filterProbes(probes).toArray
    // TODO pass a full group from client instead
    val g = new Group("temp", barcodes.map(x => new Barcode(x, "", "", "", "")).toArray)
    val mm = makeMatrix(List(g), realProbes, typ, sparseRead)
    
    //When we have obtained the data in r, it might no longer be sorted in the order that the user
    //requested. Thus we use selectNamedColumns here to force the sort order they wanted.
    
    val raw = mm.rawData.selectNamedColumns(sbc).asRows
    val rows = if (withSymbols) {
      insertAnnotations(raw)
    } else {
      raw
    }
    new ArrayList[ExpressionRow](rows)
  }

  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): Unit = 
    getSessionData.addSynthetic(test)
  
  def removeTwoGroupTests(): Unit = 
    getSessionData.removeSynthetics
    
  def prepareCSVDownload(): String = {
    import BioObjects._
    val mm = getSessionData()

    val rendered = mm.current
    if (rendered != null) {
      println("I had " + rendered.rows + " rows stored")
    }
    
    val colNames = rendered.sortedColumnMap.map(_._1)
    val rowNames = rendered.sortedRowMap.map(_._1)
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val gis = c.allGeneIds(mm.filter).mapMValues(_.identifier)      
      val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Seq.empty)).map(_.mkString(" "))
      CSVHelper.writeCSV(csvDirectory, csvUrlBase, rowNames, colNames, 
          geneIds, rendered.data.map(_.map(asScala(_))))
    })
  }

  def getGenes(limit: Int): Array[String] = {
    val mm = getSessionData()

    var rowNames = mm.current.sortedRowMap.map(_._1)
    println(rowNames.take(10))
    if (limit != -1) {
      rowNames = rowNames.take(limit)
    }
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val gis = c.allGeneIds(mm.filter)
      val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
      geneIds.flatten.map(_.identifier).toArray
    })
  }
  
  def importTargetmineLists(filter: DataFilter, user: String, pass: String,
    asProbes: Boolean): Array[StringList] = {
    val ls = TargetMine.getListService(user, pass)    
    val tmLists = ls.getAccessibleLists()
    tmLists.filter(_.getType == "Gene").map(
        l => TargetMine.asTGList(l, filterProbesAllSpecies(_))).toArray
  }

  def exportTargetmineLists(filter: DataFilter, user: String, pass: String,
    lists: Array[StringList], replace: Boolean): Unit = {
    val ls = TargetMine.getListService(user, pass)
    TargetMine.addLists(filter, ls, lists.toList, replace)
  }    
  
  def sendFeedback(name: String, email: String, feedback: String): Unit = {
    val mm = getSessionData()
    var state = "(No user state available)"
    if (mm != null) {
      if (mm.current != null) {        
    	  state = "Matrix: " + mm.current.rowKeys.size + " x " + mm.current.columnKeys.size
    	  state += "\nColumns: " + mm.current.columnKeys.mkString(", ")
    	  state += "\nData filter: " + mm.filter.toString()
      }
    }
    Feedback.send(name, email, feedback, state)
  }
  
}