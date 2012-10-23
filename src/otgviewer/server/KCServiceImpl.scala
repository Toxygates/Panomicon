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

/**
 * This servlet is responsible for obtaining and manipulating data from the Kyoto
 * Cabinet databases.
 */
class KCServiceImpl extends RemoteServiceServlet with KCService {
  import Conversions._
  import scala.collection.JavaConversions._
  import UtilsS._

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
    def data: ExprMatrix = session.getAttribute("ungroupedFiltered").asInstanceOf[ExprMatrix]
    def data_=(v: ExprMatrix) = session.setAttribute("ungroupedFiltered", v)

    def rendered: ExprMatrix = session.getAttribute("groupedFiltered").asInstanceOf[ExprMatrix]
    def rendered_=(v: ExprMatrix) = session.setAttribute("groupedFiltered", v)

    def params: DataViewParams = {
      val r = session.getAttribute("params").asInstanceOf[DataViewParams]
      if (r != null) { r } else { new DataViewParams() }
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

    val session = getSessionData() 
    val data = getExprValues(filter, barcodes(columns), filterProbes(filter, probes), typ, false)

    val groupedColumns = columns.map(c => {
      c match {
        case g: Group => {
          new ArrayVector[ExprValue]((0 until data.rows).map(r => {
            val fvs = g.getBarcodes.map(bc => data.columnMap(bc.getCode)).map(data(r, _)).filter(_.present)
            val sum = fvs.map(_.value).sum
            if (fvs.size > 0) {
              ExprValue(sum / fvs.size, 'P')
            } else {
              ExprValue(0, 'A')
            }
          }).toArray)
        }
        case b: Barcode => data.column(b.getCode)
      }
    })
    val groupedData = ExprMatrix.withColumns(groupedColumns, data)
    groupedData.columnMap = Map() ++ columns.map(_.toString).zipWithIndex

    //filter by abs. value
    def f(r: ArrayVector[ExprValue], before: Int): Boolean = r.take(before).exists(v =>
      (Math.abs(v.value) >= absValFilter - 0.0001) || (java.lang.Double.isNaN(v.value) && absValFilter == 0))

    val (ngfd, nfd) = groupedData.modifyJointly(data, _.filterRows(r => f(r, groupedData.columns)))

    session.rendered = ngfd
    session.data = nfd

    if (ngfd.rows > 0) {
      println("Stored " + ngfd.rows + " x " + ngfd.columns + " items in session")
    } else {
      System.out.println("Stored empty data in session");
    }

    val params = session.params
    params.mustSort = true
    params.filter = filter

    for (s <- syntheticColumns) {
      s match {
        case tt: Synthetic.TTest => addTTest(tt.getGroup1, tt.getGroup2)
      }
    }

    ngfd.rows
  }

  def datasetItems(offset: Int, size: Int, sortColumn: Int, ascending: Boolean): JList[ExpressionRow] = {
    def sortData(sortCol: Int, asc: Boolean, v1: ArrayVector[ExprValue],
                 v2: ArrayVector[ExprValue]): Boolean = {
      val ev1 = v1(sortCol)
      val ev2 = v2(sortCol)
      val ascFactor = if (asc) { 1 } else { -1 }
      if (ev1.call == 'A' && ev2.call != 'A') {
        false
      } else if (ev1.call != 'A' && ev2.call == 'A') {
        true
      } else {
        if (asc) { ev1.value < ev2.value } else { ev1.value >= ev2.value }
      }
    }

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

        val (grf, ugrf) = groupedFiltered.modifyJointly(session.data,
          _.sortRows((r1, r2) => sortData(sortColumn, ascending, r1, r2)))

        groupedFiltered = grf
        session.rendered = grf
        session.data = ugrf

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
      val geneIds = c.geneIds(probes)
      val geneSyms = c.geneSyms(probes)
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

    //      session.setAttribute("dataViewParams", params)
    val realProbes = filterProbes(filter, probes)
    val r = getExprValues(filter, barcodes, realProbes, typ, sparseRead)
    new ArrayList[ExpressionRow](insertAnnotations(r.asRows))
  }

  def addTTest(g1: Group, g2: Group): Unit = {
    val session = getSessionData()
    val rendered = session.rendered
    val data = session.data
    val withtt = rendered.appendTTest(data, g1.getBarcodes.map(x => data.columnMap(x.getCode)),
      g2.getBarcodes.map(x => data.columnMap(x.getCode)))
    withtt.columnMap += ((new Synthetic.TTest(g1, g2)).toString -> rendered.columns)
    session.rendered = withtt
  }

  def prepareCSVDownload(): String = {
    val session = getSessionData()
    val rendered = session.rendered
    if (rendered != null) {
      println("I had " + session.rendered.rows + " rows stored")
    }
    val colNames = rendered.columnMap.toArray.sortWith(_._2 < _._2).map(_._1)
    CSVHelper.writeCSV(rendered.annotations.map(_.probe), colNames, session.data.data)
  }

}