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
import otg.sparql.AffyProbes
import otg.sparql.BioObjects
import otg.sparql.BioObjects.makeRich
import otg.sparql.OTGSamples
import otg.sparql.Probe
import otgviewer.client.rpc.MatrixService
import otgviewer.server.ApplicationClass
import otgviewer.server.Configuration
import otgviewer.server.ExtFoldValueMatrix
import otgviewer.server.FoldValueMatrix
import otgviewer.server.ManagedMatrix
import otgviewer.server.NormalizedIntensityMatrix
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

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 */
class MatrixServiceImpl extends RemoteServiceServlet with MatrixService {
  import Conversions._
  import scala.collection.JavaConversions._
  import ScalaUtils._

  private var baseConfig: BaseConfig = _
  private var tgConfig: Configuration = _
  private var csvDirectory: String = _
  private var csvUrlBase: String = _
  private implicit var context: OTGContext = _
  var affyProbes: AffyProbes = _ 
  //TODO update mechanism
  var platforms: Map[String, Set[String]] = _
  var otgSamples: OTGSamples = _
  
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
    
    val tsCon = context.triplestoreConfig
    val ts = tsCon.triplestore
    affyProbes = new AffyProbes(ts)
    otgSamples = new OTGSamples(baseConfig)
    platforms = affyProbes.platforms.map(x => x._1 -> x._2.toSet)
  }
  
  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig =
    OTGBConfig(ts, data)

  override def destroy() {
	affyProbes.close()	
	otgSamples.close()
    super.destroy()
  }
  
  @throws(classOf[NoDataLoadedException])
  def getSessionData(): ManagedMatrix[_] = {
    val r = getThreadLocalRequest().getSession().getAttribute("matrix").asInstanceOf[ManagedMatrix[_]]
    if (r == null) {
      throw new NoDataLoadedException()
    }
    r
  }
  
  def setSessionData(m: ManagedMatrix[_]) =
    getThreadLocalRequest().getSession().setAttribute("matrix", m)

  //Should this be in sparqlService?
  //TODO: filter by platforms
  def identifiersToProbes(identifiers: Array[String], precise: Boolean): Array[String] =
    affyProbes.identifiersToProbes(context.unifiedProbes,  
        identifiers, precise).map(_.identifier).toArray

  /**
   * Filter probes for one platform.
   */
  private def filterProbes(probes: Seq[String], platform: String) = {
    if (probes.size == 0) {
      platforms(platform)
    } else {
      println(s"Filter ${probes}")
      val r = probes.filter(p => platforms(platform).contains(p))
      println(s"Result $r")
      r
    }
  }

  /**
   * Filter probes for a number of platforms.
   */
  private def filterProbes(probes: Seq[String], platforms: Iterable[String]): Seq[String] = 
    platforms.toSeq.flatMap(pf => filterProbes(probes, pf))

  /**
   * Filter probes for all platforms.
   */
  private def filterProbesAllPlatforms(probes: Seq[String]): Seq[String] = {
    val pfs = platforms.keys
    filterProbes(probes, pfs)    
  }
  
  private def makeMatrix(requestColumns: Seq[Group], initProbes: Array[String], 
      typ: ValueType, sparseRead: Boolean = false): ManagedMatrix[_] = {
    val reader = if (typ == ValueType.Absolute) {
      context.absoluteDBReader
    } else {     
      context.foldsDBReader    
    }

    try {
      val enhancedCols = tgConfig.applicationClass == ApplicationClass.Adjuvant

      reader match {
        case ext: KCExtMatrixDB =>
          assert(typ == ValueType.Folds)
          new ExtFoldValueMatrix(requestColumns, ext, initProbes, sparseRead, enhancedCols)
        case db: KCMatrixDB =>
          if (typ == ValueType.Absolute) {
            new NormalizedIntensityMatrix(requestColumns, db, initProbes, sparseRead, enhancedCols)
          } else {
            new FoldValueMatrix(requestColumns, db, initProbes, sparseRead)
          }
        case _ => throw new Exception("Unexpected DB reader type")
      }
    } finally {
      reader.close()
    }
  }

//  private[this] def speciesForGroups(gs: Iterable[Group]) = 
//    gs.flatMap(_.getUnits()).map(x => asScala(x.getOrganism())).toSet
//  
  private def platformsForGroups(gs: Iterable[Group]): Iterable[String] = {
    val samples = gs.toList.flatMap(_.getSamples().map(_.getCode))
    otgSamples.platforms(samples)
  }
    
  def loadDataset(groups: JList[Group], probes: Array[String],
                  typ: ValueType, syntheticColumns: JList[Synthetic]): ManagedMatrixInfo = {
    val platforms = platformsForGroups(groups.toList)   
    val allProbes = filterProbes(List(), platforms).toArray
    //TODO use not only the first species
    val mm = makeMatrix(groups.toVector, allProbes.toArray, typ)    
    setSessionData(mm)
    selectProbes(probes)
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
      val groups = (0 until mm.info.numColumns()).map(i => mm.info.columnGroup(i))
      val ps = platformsForGroups(groups)
      val allProbes = filterProbes(List(), ps).toArray
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
    val probes = rows.map(r => Probe(r.getProbe))

    val attribs = affyProbes.withAttributes(probes)
    val pm = Map() ++ attribs.map(a => (a.identifier -> a))
    println(pm.take(5))
    
    rows.map(or => {
      if (!pm.containsKey(or.getProbe)) {
        println("missing key: " + or.getProbe)
      }
      val p = pm(or.getProbe)
      new ExpressionRow(p.identifier, p.name, p.genes.map(_.identifier).toArray,
        p.symbols.map(_.symbol).toArray, or.getValues)
    })
  }
  
  def getFullData(g: Group, probes: Array[String], sparseRead: Boolean, 
      withSymbols: Boolean, typ: ValueType): JList[ExpressionRow] = {        
//    val species = g.getUnits().map(x => asScala(x.getOrganism())).toSet
    val platforms = platformsForGroups(List(g))    
    
    val realProbes = filterProbes(probes, platforms).toArray
    val mm = makeMatrix(List(g), realProbes.toArray, typ, sparseRead)
    
    //When we have obtained the data in r, it might no longer be sorted in the order that the user
    //requested. Thus we use selectNamedColumns here to force the sort order they wanted.
    
    val raw = mm.rawData.asRows
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
    import BioObjects._
    val mm = getSessionData()

    val rendered = mm.current
    if (rendered != null) {
      println("I had " + rendered.rows + " rows stored")
    }
    
    val colNames = rendered.sortedColumnMap.map(_._1)
    val rowNames = rendered.sortedRowMap.map(_._1)

    val gis = affyProbes.allGeneIds(null).mapMValues(_.identifier)
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

    val gis = affyProbes.allGeneIds()
    val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
    geneIds.flatten.map(_.identifier).toArray
  }
  
  // TODO: pass in a preferred species, get status info back
  def importTargetmineLists(user: String, pass: String,
    asProbes: Boolean): Array[t.viewer.shared.StringList] = {
    val ls = TargetMine.getListService(user, pass)    
    val tmLists = ls.getAccessibleLists()
    tmLists.filter(_.getType == "Gene").map(
      l => {
        val tglist = TargetMine.asTGList(l, affyProbes, filterProbesAllPlatforms(_))
        if (tglist.items.size > 0) {
          val probesForCurrent = filterProbes(tglist.items, List())
          tglist.setComment(probesForCurrent.size + "");
        } else {
          tglist.setComment("0")
        }
        tglist
      }).toArray
  }

  def exportTargetmineLists(user: String, pass: String, 
      lists: Array[StringList], replace: Boolean): Unit = {
    val ls = TargetMine.getListService(user, pass)
    TargetMine.addLists(affyProbes, ls, lists.toList, replace)
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
  
}