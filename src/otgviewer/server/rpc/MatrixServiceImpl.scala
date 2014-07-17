package otgviewer.server.rpc

import java.util.ArrayList
import java.util.{List => JList}
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList
import Conversions.asScala
import Conversions.speciesFromFilter
import t.common.shared.sample.ExpressionRow
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
import otgviewer.server.CSVHelper
import otgviewer.server.Configuration
import otgviewer.server.ExtFoldValueMatrix
import otgviewer.server.Feedback
import otgviewer.server.FoldValueMatrix
import otgviewer.server.ManagedMatrix
import otgviewer.server.NormalizedIntensityMatrix
import otgviewer.server.TargetMine
import otgviewer.shared.Barcode
import otgviewer.shared.DataFilter
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.NoDataLoadedException
import otgviewer.shared.StringList
import otgviewer.shared.Synthetic
import otgviewer.shared.ValueType
import otgviewer.server.UtilsS
import t.db.kyotocabinet.KCMatrixDB
import t.db.kyotocabinet.KCExtMatrixDB
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import otg.Species.Species
import t.BaseConfig

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 */
class MatrixServiceImpl extends RemoteServiceServlet with MatrixService {
  import Conversions._
  import scala.collection.JavaConversions._
  import UtilsS._

  private var baseConfig: BaseConfig = _
  private var tgConfig: Configuration = _
  private var csvDirectory: String = _
  private var csvUrlBase: String = _
  private implicit var context: OTGContext = _
  var affyProbes: AffyProbes = _ 
  
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
    //TODO parse baseConfig directly somewhere
    baseConfig = config.baseConfig
    
    affyProbes = new AffyProbes(context.triplestoreConfig.triplestore)
  }

  override def destroy() {
	affyProbes.close()	
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
  def identifiersToProbes(filter: DataFilter, identifiers: Array[String], precise: Boolean): Array[String] =
    affyProbes.identifiersToProbes(context.unifiedProbes, filter, 
        identifiers, precise).map(_.identifier).toArray

  /**
   * Filter probes for one species.
   */
  private def filterProbes(probes: Seq[String], s: Species): Seq[String] = {
    val pmap = context.unifiedProbes
    if (probes == null || probes.size == 0) {
      pmap.tokens.toSeq
    } else {
      probes.filter(pmap.isToken)   //TODO   
    }
  }
  
  /**
   * Filter probes for a number of species.
   * If probes is null or empty, all probes for all supplied species will be returned.
   */
  private def filterProbes(probes: Seq[String], 
      species: Iterable[Species]): Map[Species, Seq[String]] = {
    Map() ++ species.map(s => (s -> filterProbes(probes, s)))
  }

  /**
   * Filter probes for all species.   
   */
  private def filterProbesAllSpecies(probes: Seq[String]): Seq[String] = {
    val pmap = context.unifiedProbes
    if (probes == null || probes.size == 0) {
      //Requesting all probes for all species is not permitted.
      List()      
    } else {
      probes.filter(pmap.isToken) //TODO
    }
  }
  
  private[this] def makeMatrix(requestColumns: Seq[Group], initProbes: Array[String], 
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

  private[this] def speciesForGroups(gs: Iterable[Group]) = 
    gs.flatMap(_.getUnits()).map(x => asScala(x.getOrganism())).toSet
  
  def loadDataset(groups: JList[Group], probes: Array[String],
                  typ: ValueType, syntheticColumns: JList[Synthetic]): ManagedMatrixInfo = {
    val species = speciesForGroups(groups) 
    val allProbes = filterProbes(null, species).toArray
    //TODO use not only the first species
    val mm = makeMatrix(groups.toVector, allProbes.head._2.toArray, typ)    
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
      val ss = speciesForGroups(groups)
      val allProbes = filterProbes(null, ss).toArray
      //TODO
      mm.selectProbes(allProbes.head._2.toArray)
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
    val species = g.getUnits().map(x => asScala(x.getOrganism())).toSet
    
    val realProbes = filterProbes(probes, species).toArray
    //TODO! use other species as well, instead of just first one
    val mm = makeMatrix(List(g), realProbes.head._2.toArray, typ, sparseRead)
    
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
  def importTargetmineLists(filter: DataFilter, user: String, pass: String,
    asProbes: Boolean): Array[StringList] = {
    val ls = TargetMine.getListService(user, pass)    
    val tmLists = ls.getAccessibleLists()
    tmLists.filter(_.getType == "Gene").map(
      l => {
        val tglist = TargetMine.asTGList(l, affyProbes, filterProbesAllSpecies(_))
        if (tglist.items.size > 0) {
          val probesForCurrent = filterProbes(tglist.items, filter.species)
          tglist.setComment(probesForCurrent.size + "");
        } else {
          tglist.setComment("0")
        }
        tglist
      }).toArray
  }

  def exportTargetmineLists(filter: DataFilter, user: String, pass: String,
    lists: Array[StringList], replace: Boolean): Unit = {
    val ls = TargetMine.getListService(user, pass)
    TargetMine.addLists(affyProbes, filter, ls, lists.toList, replace)
  }    
  
  def sendFeedback(name: String, email: String, feedback: String): Unit = {
    val mm = getSessionData()
    var state = "(No user state available)"
    if (mm != null) {
      if (mm.current != null) {        
    	  state = "Matrix: " + mm.current.rowKeys.size + " x " + mm.current.columnKeys.size
    	  state += "\nColumns: " + mm.current.columnKeys.mkString(", ")
      }
    }
    Feedback.send(name, email, feedback, state)
  }
  
}