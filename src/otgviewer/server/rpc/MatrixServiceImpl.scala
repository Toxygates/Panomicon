package otgviewer.server.rpc

import java.util.ArrayList
import java.util.{List => JList}
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Conversions.asScala
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.OTGContext
import otg.sparql.Probes
import t.sparql._
import otg.sparql.OTGSamples
import otg.sparql.Probe
import otgviewer.client.rpc.MatrixService
import t.viewer.server.Configuration
import otgviewer.server.ManagedMatrix
import otgviewer.server.TargetMine
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.NoDataLoadedException
import otgviewer.shared.Synthetic
import otgviewer.shared.ValueType
import t.BaseConfig
import t.common.shared.sample.ExpressionRow
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.kyotocabinet.KCMatrixDB
import t.viewer.server.CSVHelper
import t.viewer.server.Feedback
import t.viewer.shared.StringList
import otgviewer.server.ScalaUtils
import t.DataConfig
import otg.OTGBConfig
import t.TriplestoreConfig
import t.viewer.server.ApplicationClass
import t.viewer.server.Platforms
import t.platform.OrthologMapping
import otgviewer.server.ManagedMatrixBuilder
import otgviewer.server.MatrixMapper
import t.common.shared.probe.OrthologProbeMapper
import t.common.shared.probe.MedianValueMapper
import t.db.MatrixDBReader
import otgviewer.shared.DBUnavailableException

object MatrixServiceImpl {
  
  /**
   * Note that in some cases, using the ServletContext rather than
   * static vars is more robust.
   */
  
  var inited = false
  
  //This in particular could be put in a servlet context and shared between
  //servlets.
  var platforms: Platforms = _
  var orthologs: Iterable[OrthologMapping] = _
  var probes: Probes = _ 
  //TODO update mechanism
  var otgSamples: OTGSamples = _
  
  def staticInit(bc: BaseConfig) = synchronized {
    if (!inited) {
      val ts = bc.triplestore.triplestore

      probes = new Probes(ts)
      otgSamples = new OTGSamples(bc)
      orthologs = probes.orthologMappings
      platforms = Platforms(probes)

      inited = true
    }
  }
  
  def staticDestroy() = synchronized {
    probes.close()	
	otgSamples.close()
  }
}

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 * 
 * This is currently the only servlet that (explicitly)
 * maintains server side sessions.
 */
class MatrixServiceImpl extends RemoteServiceServlet with MatrixService {
  import Conversions._
  import scala.collection.JavaConversions._
  import ScalaUtils._
  import MatrixServiceImpl._

  private var baseConfig: BaseConfig = _
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
    tgConfig = config
    //TODO parse baseConfig directly somewhere
    baseConfig = baseConfig(config.tsConfig, config.dataConfig)
    context = config.context(baseConfig)
    staticInit(baseConfig)
  }
  
  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig =
    OTGBConfig(ts, data)

  override def destroy() {   
    super.destroy()
  }
  
  @throws(classOf[NoDataLoadedException])
  def getSessionData(): ManagedMatrix = {
    val r = getThreadLocalRequest().getSession().getAttribute("matrix").
    		asInstanceOf[ManagedMatrix]
    if (r == null) {
      throw new NoDataLoadedException()
    }
    r
  }
  
  def setSessionData(m: ManagedMatrix) =
    getThreadLocalRequest().getSession().setAttribute("matrix", m)

  //Should this be in sparqlService?
  //TODO: filter by platforms
  def identifiersToProbes(identifiers: Array[String], precise: Boolean, 
      titlePatternMatch: Boolean): Array[String] = {
    if (titlePatternMatch) {
      probes.forTitlePatterns(identifiers).map(_.identifier).toArray
    } else {
      probes.identifiersToProbes(context.unifiedProbes,
        identifiers, precise).map(_.identifier).toArray
    }
  }

  private def makeMatrix(requestColumns: Seq[Group], initProbes: Array[String], 
      typ: ValueType, sparseRead: Boolean = false): ManagedMatrix = {

    val reader = try {
      if (typ == ValueType.Absolute) {
        context.absoluteDBReader
      } else {
        context.foldsDBReader
      }
    } catch {
      case e: Exception => throw new DBUnavailableException()
    }

    try {
      val enhancedCols = tgConfig.applicationClass == ApplicationClass.Adjuvant

      reader match {
        case ext: KCExtMatrixDB =>
          assert(typ == ValueType.Folds)
          ManagedMatrixBuilder.buildExtFold(requestColumns, ext, initProbes, sparseRead, enhancedCols)          
        case db: KCMatrixDB =>
          if (typ == ValueType.Absolute) {
            ManagedMatrixBuilder.buildNormalized(requestColumns, db, initProbes, sparseRead, enhancedCols)
          } else {
            ManagedMatrixBuilder.buildFold(requestColumns, db, initProbes, sparseRead)
          }
        case _ => throw new Exception("Unexpected DB reader type")
      }
    } finally {
      reader.release
    }
  }
  
  private def platformsForGroups(gs: Iterable[Group]): Iterable[String] = {
    val samples = gs.toList.flatMap(_.getSamples().map(_.getCode))
    otgSamples.platforms(samples)
  }
    
  def loadDataset(groups: JList[Group], probes: Array[String],
                  typ: ValueType, syntheticColumns: JList[Synthetic]): ManagedMatrixInfo = {
    val pfs = platformsForGroups(groups.toList)   
    val allProbes = platforms.filterProbes(List(), pfs).toArray 
    val mm = makeMatrix(groups.toVector, allProbes, typ)
    setSessionData(mm)        
    selectProbes(probes)
    mapper(groups) match {
      case Some(m) =>
        val mapped = m.convert(mm)
        setSessionData(mapped)
        mapped.info
      case None =>
        mm.info
    }
  }

  @throws(classOf[NoDataLoadedException])
  def selectProbes(probes: Array[String]): ManagedMatrixInfo = {
    if (probes != null) {
      println("Refilter probes: " + probes.length)      
    }
    val mm = getSessionData
    
//    mm.filterData(Some(absValFilter))	
    if (probes != null && probes.length > 0) {
    	mm.selectProbes(probes)
    } else {
      val groups = (0 until mm.info.numDataColumns()).map(i => mm.info.columnGroup(i))
      val ps = platformsForGroups(groups)
      val allProbes = platforms.filterProbes(List(), ps).toArray
      mm.selectProbes(allProbes)
    }
    mm.info
  }
  
  @throws(classOf[NoDataLoadedException])
  def setColumnThreshold(column: Int, threshold: java.lang.Double): ManagedMatrixInfo = {
    val mm = getSessionData 
    println(s"Filter column $column at $threshold")
    mm.setFilterThreshold(column, threshold)
    mm.info
  }

  @throws(classOf[NoDataLoadedException])
  def datasetItems(offset: Int, size: Int, sortColumn: Int,
    ascending: Boolean): JList[ExpressionRow] = {

    println("SortCol: " + sortColumn + " asc: " + ascending)
    val session = getSessionData()
    if (sortColumn != session.sortColumn || ascending != session.sortAscending) {
      session.sort(sortColumn, ascending)
    }
    val mm = session.current
    
    new ArrayList[ExpressionRow](
      insertAnnotations(mm.asRows.drop(offset).take(size)))     
  }

  /**
   * Dynamically obtain annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  private def insertAnnotations(rows: Seq[ExpressionRow]): Seq[ExpressionRow] = {
    val atomised = Map() ++ rows.map(r => {
      val p = r.getProbe
      if (p.contains("/")) {
        //TODO avoid hardcoding this separator
        val atomics = p.split("/")
        p -> atomics
      } else {
        p -> Array(p)
      }
    })

    val attribs = probes.withAttributes(atomised.flatMap(_._2.map(Probe(_))))
    val pm = Map() ++ attribs.map(a => (a.identifier -> a))
    println(pm.take(5))
    
    rows.map(or => {
      val atomics = atomised(or.getProbe)
      
      val ps = atomics.flatMap(pm.get(_))
      new ExpressionRow(ps.map(_.identifier).mkString("/ "),
          ps.map(_.name).mkString("/ "),
          ps.flatMap(_.genes.map(_.identifier)),
          ps.flatMap(_.symbols.map(_.symbol)),
          or.getValues)            
    })
  }
  
  def getFullData(g: Group, probes: Array[String], sparseRead: Boolean, 
      withSymbols: Boolean, typ: ValueType): JList[ExpressionRow] = {        
//    val species = g.getUnits().map(x => asScala(x.getOrganism())).toSet
    val pfs = platformsForGroups(List(g))    
    
    val realProbes = platforms.filterProbes(probes, pfs).toArray
    val mm = makeMatrix(List(g), realProbes.toArray, typ, sparseRead)
    
    //When we have obtained the data, it might no longer be sorted in the order that the user
    //requested. Thus we use selectNamedColumns here to force the sort order they wanted.
    
    val raw = mm.rawData.selectNamedColumns(g.getSamples().map(_.id())).asRows    
    val rows = if (withSymbols) {
      insertAnnotations(raw)
    } else {
      raw
    }
    new ArrayList[ExpressionRow](rows)
  }

  @throws(classOf[NoDataLoadedException])
  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): Unit = 
    getSessionData.addSynthetic(test)
  
  @throws(classOf[NoDataLoadedException])
  def removeTwoGroupTests(): Unit = 
    getSessionData.removeSynthetics
    
  @throws(classOf[NoDataLoadedException])
  def prepareCSVDownload(): String = {
    val mm = getSessionData()

    val rendered = mm.current
    if (rendered != null) {
      println("I had " + rendered.rows + " rows stored")
    }
    
    val colNames = rendered.sortedColumnMap.map(_._1)
    val rowNames = rendered.sortedRowMap.map(_._1)

    val gis = probes.allGeneIds(null).mapMValues(_.identifier)
    val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Seq.empty)).map(_.mkString(" "))
    CSVHelper.writeCSV(csvDirectory, csvUrlBase, rowNames, colNames,
      geneIds, rendered.data.map(_.map(asScala(_))))
  }

  @throws(classOf[NoDataLoadedException])
  def getGenes(limit: Int): Array[String] = {
    val mm = getSessionData()

    var rowNames = mm.current.sortedRowMap.map(_._1)
    println(rowNames.take(10))
    if (limit != -1) {
      rowNames = rowNames.take(limit)
    }

    val gis = probes.allGeneIds()
    val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
    geneIds.flatten.map(_.identifier).toArray
  }
  
  protected def feedbackReceivers: String = "johan@monomorphic.org,kenji@nibio.go.jp,y-igarashi@nibio.go.jp"
  
  def sendFeedback(name: String, email: String, feedback: String): Unit = {
    val mm = getSessionData()
    var state = "(No user state available)"
    if (mm != null) {
      if (mm.current != null) {        
    	  state = "Matrix: " + mm.current.rowKeys.size + " x " + mm.current.columnKeys.size
    	  state += "\nColumns: " + mm.current.columnKeys.mkString(", ")
      }
    }    
    Feedback.send(name, email, feedback, state, feedbackReceivers)
  }
  
  protected def mapper(groups: Iterable[Group]): Option[MatrixMapper] = {
    val os = groups.flatMap(_.collect("organism")).toSet
    println("Detected species in groups: " + os)
    if (os.size > 1) {
      val pm = new OrthologProbeMapper(orthologs.head)
      val vm = MedianValueMapper
      Some(new MatrixMapper(pm, vm))      
    } else {
      None
    }
  }
}