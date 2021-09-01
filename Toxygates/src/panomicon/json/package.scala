package panomicon.json

import t.server.viewer.matrix.ManagedMatrix
import t.shared.viewer.{ColumnFilter, FilterType}
import upickle.default.{macroRW, ReadWriter => RW}

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
case class NetworkParams(matrix1: MatrixParams, matrix2: MatrixParams,
                         associationSource: String, associationLimit: String = null)
