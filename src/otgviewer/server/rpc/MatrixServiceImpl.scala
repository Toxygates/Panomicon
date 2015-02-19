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
import t.viewer.server.Platforms
import t.platform.OrthologMapping
import otgviewer.server.ManagedMatrixBuilder
import otgviewer.server.MatrixMapper
import t.common.shared.probe.OrthologProbeMapper
import t.common.shared.probe.MedianValueMapper
import t.db.MatrixDBReader
import otgviewer.shared.DBUnavailableException
import t.common.shared.DataSchema
import otgviewer.shared.OTGSchema
import t.platform.Probe
import t.db.MatrixContext
import otgviewer.shared.FullMatrix

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
      probes = new Probes(bc.triplestore)
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
  protected implicit var context: OTGContext = _
  protected implicit var ocontext: otg.Context = _

  protected val schema: DataSchema = new OTGSchema()
  
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
    //TODO revise where this gets created
    ocontext = otg.Context(baseConfig)
    context = ocontext.matrix
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
      typ: ValueType, sparseRead: Boolean = false, fullLoad: Boolean = false): ManagedMatrix = {

    val reader = try {
      if (typ == ValueType.Absolute) {
        context.absoluteDBReader
      } else {
        context.foldsDBReader
      }
    } catch {
      case e: Exception => throw new DBUnavailableException()
    }

    val pfs = platformsForGroups(requestColumns)
    val multiPlat = pfs.size > 1
    
    try {
      val enhancedCols = !multiPlat

      reader match {
        case ext: KCExtMatrixDB =>
          assert(typ == ValueType.Folds)
          ManagedMatrixBuilder.buildExtFold(requestColumns, ext, initProbes, 
              sparseRead, fullLoad, enhancedCols)          
        case db: KCMatrixDB =>
          if (typ == ValueType.Absolute) {
            ManagedMatrixBuilder.buildNormalized(requestColumns, db, initProbes, 
                sparseRead, fullLoad, enhancedCols)
          } else {
            ManagedMatrixBuilder.buildFold(requestColumns, db, initProbes, 
                sparseRead, fullLoad)
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
    
  private def platformsForProbes(ps: Iterable[String]): Iterable[String] = 
    ps.flatMap(platforms.platformForProbe(_))
  
  private def applyMapper(groups: JList[Group], mm: ManagedMatrix): ManagedMatrix = {
     mapper(groups) match {
      case Some(m) => m.convert(mm)        
      case None => mm
     }        
  }
  
  def loadDataset(groups: JList[Group], probes: Array[String],
                  typ: ValueType, syntheticColumns: JList[Synthetic]): ManagedMatrixInfo = {
    val pfs = platformsForGroups(groups.toList)   
    val fProbes = platforms.filterProbes(probes, pfs).toArray 
    val mm = makeMatrix(groups.toVector, fProbes, typ)
    setSessionData(mm)    
    mm.info.setPlatforms(pfs.toArray)
//    selectProbes(probes)
    val mm2 = applyMapper(groups, mm)
    setSessionData(mm2)
    mm2.info
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
    val mergeMode = session.info.getPlatforms.size > 1
    
    new ArrayList[ExpressionRow](
      insertAnnotations(mm.asRows.drop(offset).take(size), mergeMode))     
  }

  //this is probably quite inefficient
  private def withCount[T](xs: Seq[T]): Iterable[(T, Int)] =     
    xs.distinct.map(x => (x, xs.count(_ == x)))      
  
  private def prbCount(n: Int) = {
    if (n == 0) {
      "No probes"
    } else if (n == 1) {
      "1 probe"
    } else {
      s"$n probes"
    }
  }
    
  /**
   * Dynamically obtain annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  private def insertAnnotations(rows: Seq[ExpressionRow], mergeMode: Boolean): Seq[ExpressionRow] = {
    val allAtomics = rows.flatMap(_.getAtomicProbes)

    val attribs = probes.withAttributes(allAtomics.map(Probe(_)))
    val pm = Map() ++ attribs.map(a => (a.identifier -> a))
    println(pm.take(5))
    
    rows.map(or => {
      val atomics = or.getAtomicProbes() 
      val ps = atomics.flatMap(pm.get(_))
      
      if (mergeMode) {               
    	val expandedGenes = ps.flatMap(p => 
    	  p.genes.map(g => (schema.platformSpecies(p.platform), g.identifier)))
        val expandedSymbols = ps.flatMap(p => 
          p.symbols.map(schema.platformSpecies(p.platform) + ":" + _.symbol))
      
        val r = new ExpressionRow(atomics.mkString("/ "),
          atomics,
          ps.map(p => p.name).mkString("/ "),
          expandedGenes.map(_._2).distinct,
          withCount(expandedSymbols).map(x => s"${x._1} (${prbCount(x._2)})").toArray,    
          or.getValues)
    	
    	val gils = withCount(expandedGenes).map(x => s"${x._1._1 + ":" + x._1._2} (${prbCount(x._2)})").toArray
    	r.setGeneIdLabels(gils)
    	r
      } else {      
        new ExpressionRow(ps.map(_.identifier).mkString("/ "),
          atomics,
          ps.map(p => p.name).mkString("/ "),
          ps.flatMap(_.genes.map(_.identifier)),
          ps.flatMap(_.symbols.map(_.symbol)),             
          or.getValues)
      }
    })
  }
  
  def getFullData(gs: JList[Group], rprobes: Array[String], sparseRead: Boolean, 
      withSymbols: Boolean, typ: ValueType): FullMatrix = {
    val sgs = Vector() ++ gs
    val pfs = platformsForGroups(sgs)    
    
    val realProbes = platforms.filterProbes(rprobes, pfs).toArray
    val mm = makeMatrix(sgs, realProbes.toArray, typ, sparseRead, true)
    mm.info.setPlatforms(pfs.toArray)
    
    val mapper = mapperForProbes(realProbes)    
    val mm2 = mapper.map(mx => mx.convert(mm)).getOrElse(mm)

    val raw = if (sgs.size == 1) {
      //break out each individual sample
      mm2.rawData.selectNamedColumns(sgs(0).getSamples().map(_.id())).asRows
    } else {
      mm2.current.selectNamedColumns(sgs.map(_.getName)).asRows
    }
    
    val rows = if (withSymbols) {
      insertAnnotations(raw, pfs.size > 1)
    } else {
      //TODO: this should be a lazy val
      //TODO: some clients need neither "symbols"/annotations nor geneIds
      val gis = probes.allGeneIds(null).mapMValues(_.identifier).
        mapKValues(_.identifier)
      raw.map(or => {        
        new ExpressionRow(or.getProbe, or.getAtomicProbes, or.getTitle,
            or.getAtomicProbes.flatMap(p => gis.getOrElse(p, Seq.empty)),
            or.getGeneSyms, or.getValues)
      })      
    }
    new FullMatrix(mm2.info, new ArrayList[ExpressionRow](rows))    
  }

  @throws(classOf[NoDataLoadedException])
  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): Unit = 
    getSessionData.addSynthetic(test)
  
  @throws(classOf[NoDataLoadedException])
  def removeTwoGroupTests(): Unit = 
    getSessionData.removeSynthetics
    
  @throws(classOf[NoDataLoadedException])  
  def prepareCSVDownload(individualSamples: Boolean): String = {
    val mm = getSessionData()
    val mat = if (individualSamples &&
        mm.rawUngroupedMat != null && mm.current != null) {
      val info = mm.info
      val ug = mm.rawUngroupedMat.selectNamedRows(mm.current.rowKeys.toSeq)
      val gs = (0 until info.numDataColumns()).map(g => info.columnGroup(g))
      //Help the user by renaming the columns.
      //Prefix sample IDs by group IDs.
      val parts = gs.map(g => { 
        val ids = g.getTreatedSamples.map(_.getCode)
        val sel = ug.selectNamedColumns(ids)
        val newNames = Map() ++ sel.columnMap.map(x => (g.getName + ":" + x._1 -> x._2))
        sel.copyWith(sel.data, sel.rowMap, newNames)
      })
      parts.reduce(_ adjoinRight _)      
    } else {
      mm.current
    }
    
    if (mat != null) {
      println("I had " + mat.rows + " rows stored")
    }
    
    val colNames = mat.sortedColumnMap.map(_._1)
    val rows = mat.asRows    
    //TODO shared logic with e.g. insertAnnotations, extract
    val rowNames = rows.map(_.getAtomicProbes.mkString("/"))

    val gis = probes.allGeneIds(null).mapMValues(_.identifier)
    val atomics = rows.map(_.getAtomicProbes())
    val geneIds = atomics.map(row => 
      row.flatMap(at => gis.getOrElse(Probe(at), Seq.empty))).map(_.distinct.mkString(" "))
    CSVHelper.writeCSV(csvDirectory, csvUrlBase, rowNames, colNames,
      geneIds, mat.data.map(_.map(asScala(_))))
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
  
  protected def feedbackReceivers: String = "jtnystrom@gmail.com,kenji@nibio.go.jp,y-igarashi@nibio.go.jp"
  
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
  
  private lazy val standardMapper = {
      val pm = new OrthologProbeMapper(orthologs.head)
      val vm = MedianValueMapper
      new MatrixMapper(pm, vm)   
  }
  
  protected def mapper(groups: Iterable[Group]): Option[MatrixMapper] = {
    val os = groups.flatMap(_.collect("organism")).toSet
    println("Detected species in groups: " + os)
    if (os.size > 1) {
       Some(standardMapper)
    } else {
      None
    }
  }
  
  protected def mapperForProbes(ps: Iterable[String]): Option[MatrixMapper] = {
    val pfs = platformsForProbes(ps).toSet
    if (pfs.size > 1) {
      Some(standardMapper)      
    } else {
      None
    }    
  }
}