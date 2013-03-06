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
import otgviewer.shared.DataColumn
import otgviewer.shared.ValueType
import otgviewer.shared.ExpressionRow
import otgviewer.shared.Group
import otg.ExprValue
import otgviewer.shared.Barcode
import friedrich.statistics.ArrayVector
import javax.servlet.http.HttpSession
import otg.sparql.AffyProbes
import otg.CSVHelper
import otgviewer.shared.Series
import otg.OTGSeriesQuery
import otgviewer.shared.ExpressionValue
import otg.sparql._
import otg.OTGCabinet

/**
 * This servlet is responsible for obtaining and manipulating data from the Kyoto
 * Cabinet databases.
 */
class KCServiceImpl extends RemoteServiceServlet with KCService {
  import Conversions._
  import scala.collection.JavaConversions._
  import UtilsS._

  class DataViewParams {
    var sortAsc: Boolean = _
    var sortColumn: Int = _
    var mustSort: Boolean = _
    var filter: DataFilter = _
  }

  private var foldsDB: DB = _
  private var absDB: DB = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    val homePath = System.getProperty("otg.home")
    foldsDB = OTGQueries.open(homePath + "/otgf.kct")
    absDB = OTGQueries.open(homePath + "/otg.kct")    
    println("KC databases are open")
  }

  override def destroy() {
    println("Closing KC databases")
    foldsDB.close()
    absDB.close()
    super.destroy()
  }

  private def getSessionData() = new SessionData(getThreadLocalRequest().getSession())

  private class SessionData(val session: HttpSession) {
    def ungroupedUnfiltered: ExprMatrix = session.getAttribute("ungroupedUnfiltered").asInstanceOf[ExprMatrix]
    def ungroupedUnfiltered_=(v: ExprMatrix) = session.setAttribute("ungroupedUnfiltered", v)

    def ungroupedFiltered: ExprMatrix = session.getAttribute("ungroupedFiltered").asInstanceOf[ExprMatrix]
    def ungroupedFiltered_=(v: ExprMatrix) = session.setAttribute("ungroupedFiltered", v)

    def rendered: ExprMatrix = session.getAttribute("groupedFiltered").asInstanceOf[ExprMatrix]
    def rendered_=(v: ExprMatrix) = session.setAttribute("groupedFiltered", v)

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
  //Is the separate OTGMisc object needed?
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
    val sorted = OTGQueries.sortBarcodes(barcodes)
    val data = OTGQueries.presentValuesByBarcodesAndProbes(db, sorted, probes, sparseRead, filter)
    val r = ExprMatrix.withRows(data.map(_.toSeq)) //todo
    r.annotations = probes.map(ExprMatrix.RowAnnotation(_, null, null, null)).toArray
    r.columnMap = Map() ++ sorted.zipWithIndex
    r.rowMap = Map() ++ probes.zipWithIndex
    r
  }

  def loadDataset(filter: DataFilter, columns: JList[DataColumn], probes: Seq[String],
                  typ: ValueType, absValFilter: Double, syntheticColumns: JList[Synthetic]): Int = {

    def barcodes(columns: Seq[DataColumn]): Seq[String] = {
      columns.flatMap(_ match {
        case g: Group   => g.getBarcodes
        case b: Barcode => Vector(b)
        case _          => Vector()
      }).map(_.getCode)
    }

    val filtered = filterProbes(filter, null).toSeq
    val session = getSessionData()
    
    //load with all probes for this filter
    val data = getExprValues(filter, barcodes(columns.toVector), filtered, typ, false)

    session.ungroupedUnfiltered = data
    refilterData(filter, columns, probes, absValFilter, syntheticColumns)
  }

  private def makeGroups(data: ExprMatrix, columns: Seq[DataColumn]) = {    
    val groupedColumns = columns.map(_ match {
      case g: Group => {
        new ArrayVector[ExprValue]((0 until data.rows).map(r => {
          val vs = g.getBarcodes.map(bc => data.columnMap(bc.getCode)).map(data(r, _))
          ExprValue.presentMean(vs, "")
        }).toArray)
      }
      case b: Barcode => data.column(b.getCode)
    })
    
    val r = ExprMatrix.withColumns(groupedColumns, data)
    r.columnMap = Map() ++ columns.map(_.toString).zipWithIndex
    r
  }
  
  def refilterData(filter: DataFilter, columns: JList[DataColumn], probes: Seq[String], absValFilter: Double,
                   syntheticColumns: JList[Synthetic]): Int = {
    println("Refilter probes: " + probes)
    if (probes != null) {
      println("Length: " + probes.size)
    }

    val session = getSessionData()
    val data = session.ungroupedUnfiltered

    var groupedData = makeGroups(data, columns)

    val filteredProbes = filterProbes(filter, probes)
//    //pick out rows that correspond to the selected probes only
//    groupedData = groupedData.selectNamedRows(filtered)

    //filter by abs. value
    def f(r: ArrayVector[ExprValue], before: Int): Boolean = r.take(before).exists(v =>
      (Math.abs(v.value) >= absValFilter - 0.0001) || (java.lang.Double.isNaN(v.value) && absValFilter == 0))

    //Pick out rows that correspond to the selected probes only, and filter them by absolute value
    val (ngfd, nfd) = groupedData.modifyJointly(data, 
        _.selectNamedRows(filteredProbes.toSeq).filterRows(r => f(r, groupedData.columns)))

    session.rendered = ngfd
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
    def sortData(v1: ArrayVector[ExprValue],
                 v2: ArrayVector[ExprValue]): Boolean = {
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

        val (grf, ugrf) = groupedFiltered.modifyJointly(session.ungroupedFiltered,
          _.sortRows(sortData))

        groupedFiltered = grf
        session.rendered = grf
        session.ungroupedFiltered = ugrf
      }
      new ArrayList[ExpressionRow](insertAnnotations(groupedFiltered.asRows.drop(offset).take(size)))
    } else {
      new ArrayList[ExpressionRow]()
    }
  }

  /**
   * Dynamically obtain annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  private def insertAnnotations(rows: Seq[ExpressionRow]): Seq[ExpressionRow] = {
    val probes = rows.map(r => Probe(r.getProbe))
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val probeTitles = c.titles(probes)
      //todo: collapse the two gene lookup functions
      val geneIds = c.geneIds(probes)
      val geneSyms = c.geneSyms(probes)
      
      rows.map(or => {
        val p = or.getProbe
        new ExpressionRow(p, probeTitles(p), geneIds(p).map(_.identifier).toArray,
            geneSyms(p).map(_.symbol).toArray, or.getValues)
      })      
    })
  }

  def getFullData(filter: DataFilter, barcodes: JList[String], probes: Array[String],
                  typ: ValueType, sparseRead: Boolean): JList[ExpressionRow] = {
    val session = getSessionData()
    val p = session.params
    p.filter = filter

    val realProbes = filterProbes(filter, probes)
    val r = getExprValues(filter, barcodes, realProbes, typ, sparseRead)
    //When we have obtained the data in r, it may no longer be sorted in the order that the user
    //requested. Thus we use selectNamedRows here to force the sort order they wanted.
    new ArrayList[ExpressionRow](insertAnnotations(r.selectNamedColumns(barcodes).asRows))
  }

  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): Unit = {
    val session = getSessionData()
    val rendered = session.rendered
    val data = session.ungroupedFiltered
    val g1 = test.getGroup1
    val g2 = test.getGroup2
    val withTest = test match {
      case ut: Synthetic.UTest => {
        rendered.appendUTest(data, g1.getBarcodes.map(_.getCode),
          g2.getBarcodes.map(_.getCode))
      }
      case tt: Synthetic.TTest => {
        rendered.appendTTest(data, g1.getBarcodes.map(_.getCode),
          g2.getBarcodes.map(_.getCode))
      }
    }
    withTest.columnMap += (test.getShortTitle() -> rendered.columns)
    session.rendered = withTest

  }

  def prepareCSVDownload(): String = {
    val session = getSessionData()
    val rendered = session.rendered
    if (rendered != null) {
      println("I had " + session.rendered.rows + " rows stored")
    }
    
    val colNames = rendered.sortedColumnMap.map(_._1)
    val rowNames = rendered.sortedRowMap.map(_._1)
    useConnector(AffyProbes, (c: AffyProbes.type) => {
      val gis = c.allGeneIds(session.params.filter)      
      val geneIds = rowNames.map(gis.getOrElse(_, Seq.empty)).map(_.mkString(" "))
      CSVHelper.writeCSV(rowNames, colNames, geneIds, session.rendered.data)
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
      val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
      geneIds.allValues.map(_.identifier).toArray
//      geneIds.map(_.identifier).toArray
    })
  }



}