package panomicon.json

import t.db.BasicExprValue
import t.server.viewer.intermine.GeneList
import t.server.viewer.matrix.{ExpressionRow, ManagedMatrix}
import t.shared.viewer.mirna.MirnaSource
import t.shared.viewer.{ColumnFilter, FilterType}
import t.sparql.{Batch, Dataset}
import upickle.default.{macroRW, readwriter, ReadWriter => RW}

import java.text.SimpleDateFormat
import java.util.Date

object Group { implicit val rw: RW[Group] = macroRW }
case class Group(name: String, sampleIds: Seq[String])

object FilterSpec { implicit val rw: RW[FilterSpec] = macroRW }
case class FilterSpec(column: String, `type`: String, threshold: Double)

object SortSpec { implicit val rw: RW[SortSpec] = macroRW }
case class SortSpec(field: String, dir: String)

object MatrixParams { implicit val rw: RW[MatrixParams] = macroRW }
case class MatrixParams(groups: Seq[Group], probes: Seq[String] = Seq(),
                        filtering: Seq[FilterSpec] = Seq(),
                        sorter: SortSpec = null) {

  def applyFilters(mat: ManagedMatrix): Unit = {
    for (f <- filtering) {
      val col = f.column
      val idx = mat.info.findColumnByName(col)
      if (idx != -1) {
        //Filter types can be, e.g.: ">", "<", "|x| >", "|x| <"
        val filt = new ColumnFilter(f.threshold, FilterType.parse(f.`type`))
        println(s"Filter for column $idx: $filt")
        mat.setFilter(idx, filt)
      } else {
        Console.err.println(s"Unable to find column $col. Filtering will not apply to this column.")
      }
    }
  }

  def applySorting(mat: ManagedMatrix): Unit = {
    val defaultSortCol = 0
    val defaultSortAsc = false
    Option(sorter) match {
      case Some(sort) =>
        val idx = mat.info.findColumnByName(sort.field)
        if (idx != -1) {
          val asc = sort.dir == "asc"
          mat.sort(idx, asc)
        } else {
          Console.err.println(s"Unable to find column ${sort.field}. Sorting will not apply to this column.")
          mat.sort(defaultSortCol, defaultSortAsc)
        }
      case None =>
        mat.sort(defaultSortCol, defaultSortAsc)
    }
  }
}

object NetworkParams { implicit val rw: RW[NetworkParams] = macroRW }
case class NetworkParams(groups1: Seq[Group], probes1: Seq[String] = Seq(),
                         groups2: Seq[Group],
                         associationSource: String, associationLimit: String = null) {
  import java.lang.{Double => JDouble}

  def mirnaSource = {
    val limit = Option(associationLimit).map(x => JDouble.parseDouble(x): JDouble)
    new MirnaSource(associationSource, "", false,
      limit.getOrElse(null: JDouble),
      0, null, null, null)
  }

  def matrix1: MatrixParams = MatrixParams(groups1, probes1)
  def matrix2: MatrixParams = MatrixParams(groups2)
}

object Encoders {
  // date should be added, either ISO 8601 or millis since 1970
  // https://stackoverflow.com/a/15952652/689356
  //Work in progress - this supposedly conforms to ISO 8601
  val jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dtRW = readwriter[String].bimap[Date](
    output => jsonDateFormat.format(output),
    input =>
      try {
        jsonDateFormat.parse(input)
      } catch {
        case _: Exception => new Date(0)
      }
  )

  implicit val bevRw: RW[BasicExprValue] = macroRW
  implicit val erRw: RW[ExpressionRow] = macroRW
  implicit val dsRW: RW[Dataset] = macroRW
  implicit val batRW: RW[Batch] = macroRW
  implicit val glRW: RW[GeneList] = macroRW
}
