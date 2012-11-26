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
import otg.OTGMisc
import otgviewer.shared.ExpressionRow
import otgviewer.shared.Group
import otg.ExprValue
import otgviewer.shared.Barcode
import friedrich.statistics.ArrayVector
import javax.servlet.http.HttpSession
import otg.B2RAffy
import otg.CSVHelper
import otgviewer.shared.Series
import otg.OTGSeriesQuery
import otgviewer.shared.ExpressionValue

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
  private var seriesDB: DB = _

  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    val homePath = System.getProperty("otg.home")
    foldsDB = OTGQueries.open(homePath + "/otgf.kct")
    absDB = OTGQueries.open(homePath + "/otg.kct")
    seriesDB = OTGSeriesQuery.open(homePath + "/otgfs.kct")
    println("KC databases are open")
  }

  override def destroy() {
    println("Closing KC databases")
    foldsDB.close()
    absDB.close()
    seriesDB.close()
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
    OTGMisc.identifiersToProbes(filter, identifiers, precise)

  private def filterProbes(filter: DataFilter, probes: Array[String]) =
    if (probes == null || probes.size == 0) {
      OTGQueries.probeIds(filter)
    } else {
      OTGQueries.filterProbes(probes, filter)
    }

  private def getDB(typ: ValueType) = typ match {
    case ValueType.Folds    => foldsDB
    case ValueType.Absolute => absDB
  }

  private def getExprValues(filter: DataFilter, barcodes: Iterable[String], probes: Array[String],
                            typ: ValueType, sparseRead: Boolean): ExprMatrix = {
    val db = getDB(typ)
    val data = OTGQueries.presentValuesByBarcodesAndProbes(db, barcodes.toSeq, probes, sparseRead, filter)
    val r = ExprMatrix.withRows(data.map(_.toSeq).toSeq) //todo
    r.annotations = probes.map(ExprMatrix.RowAnnotation(_, null, null, null)).toArray
    r.columnMap = Map() ++ barcodes.zipWithIndex
    r.rowMap = Map() ++ probes.zipWithIndex
    r
  }

  def loadDataset(filter: DataFilter, columns: JList[DataColumn], probes: Array[String],
                  typ: ValueType, absValFilter: Double, syntheticColumns: JList[Synthetic]): Int = {

    def barcodes(columns: Iterable[DataColumn]): Iterable[String] = {
      columns.flatMap(_ match {
        case g: Group   => g.getBarcodes
        case b: Barcode => Vector(b)
        case _          => Vector()
      }).map(_.getCode)
    }

    val filtered = filterProbes(filter, null)
    val session = getSessionData()
    //load with all probes for this filter
    val data = getExprValues(filter, barcodes(columns), filtered, typ, false)

    session.ungroupedUnfiltered = data
    refilterData(filter, columns, probes, absValFilter, syntheticColumns)
  }
  
  def refilterData(filter: DataFilter, columns: JList[DataColumn], probes: Array[String], absValFilter: Double,
                   syntheticColumns: JList[Synthetic]): Int = {
    println("Refilter probes: " + probes)
    if (probes != null) {
      println("Length: " + probes.size)
    }
    
    val session = getSessionData()
    val data = session.ungroupedUnfiltered
    
    val groupedColumns = columns.map(_ match {
        case g: Group => {
          new ArrayVector[ExprValue]((0 until data.rows).map(r => {
            val vs = g.getBarcodes.map(bc => data.columnMap(bc.getCode)).map(data(r, _))
            ExprValue.presentMean(vs, "")            
          }).toArray)
        }
        case b: Barcode => data.column(b.getCode)
      })
    
    var groupedData = ExprMatrix.withColumns(groupedColumns, data)
    groupedData.columnMap = Map() ++ columns.map(_.toString).zipWithIndex    
    
    val filtered = filterProbes(filter, probes)
    //pick out rows that correspond to the selected probes only
    val selectedRows = filtered.map(groupedData.row(_))
    groupedData = ExprMatrix.withRows(selectedRows, data)
    
    //filter by abs. value
    def f(r: ArrayVector[ExprValue], before: Int): Boolean = r.take(before).exists(v =>
      (Math.abs(v.value) >= absValFilter - 0.0001) || (java.lang.Double.isNaN(v.value) && absValFilter == 0))

    val (ngfd, nfd) = groupedData.modifyJointly(data, _.filterRows(r => f(r, groupedData.columns)))

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
  private def insertAnnotations(rows: Iterable[ExpressionRow]): Iterable[ExpressionRow] = {
    val probes = rows.map(_.getProbe).toArray
    useConnector(B2RAffy, (c: B2RAffy.type) => {
      val probeTitles = c.titles(probes)
      val geneIds = c.geneIds(probes).map(_.toArray)
      val geneSyms = c.geneSyms(probes).map(_.toArray)
      rows.zip(probeTitles).zip(geneIds).zip(geneSyms).map(r => {
        val or = r._1._1._1
        new ExpressionRow(or.getProbe, r._1._1._2, r._1._2, r._2, or.getValues)
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
    new ArrayList[ExpressionRow](insertAnnotations(r.asRows))
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
    withTest.columnMap += (test.toString -> rendered.columns)
    session.rendered = withTest
  }

  def prepareCSVDownload(): String = {
    val session = getSessionData()
    val rendered = session.rendered
    if (rendered != null) {
      println("I had " + session.rendered.rows + " rows stored")
    }
    val colNames = rendered.columnMap.toArray.sortWith(_._2 < _._2).map(_._1)
    CSVHelper.writeCSV(rendered.annotations.map(_.probe), colNames, session.ungroupedFiltered.data)
  }

  def getSingleSeries(filter: DataFilter, probe: String, timeDose: String, compound: String): Series = {
    OTGSeriesQuery.getSeries(seriesDB, asScala(filter, new Series("", probe, timeDose, compound, Array.empty))).head
  }

  def getSeries(filter: DataFilter, probes: Array[String], timeDose: String, compounds: Array[String]): JList[Series] = {
    val validated = OTGMisc.identifiersToProbesQuick(filter, probes, true)
    val ss = validated.flatMap(p => 
      compounds.flatMap(c =>
      OTGSeriesQuery.getSeries(seriesDB, asScala(filter, new Series("", p, timeDose, c, Array.empty)))
      ))
    val jss = ss.map(asJava(_))
    for (s <- ss) {
      println(s)
    }

    new ArrayList[Series](asJavaCollection(jss))
  }

}