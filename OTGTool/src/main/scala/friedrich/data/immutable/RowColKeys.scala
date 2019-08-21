/**
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Part of the Friedrich bioinformatics framework.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */

package friedrich.data.immutable
import friedrich.data._

/**
 * An immutable row/col map that can be split, subsetted etc.
 * by creating new copies.
 */
trait RowColKeys[T, V <: Seq[T], Row, Column]
extends friedrich.data.RowColKeys[T, V, Row, Column] {
  this: AbstractMatrix[_, T, V] =>

  val columnMap: Map[Column, Int]
  val rowMap: Map[Row, Int]

  private val _invColumnMap = Map() ++ columnMap.map(x => x._2 -> x._1)
  private val _invRowMap = Map() ++ rowMap.map(x => x._2 -> x._1)

  def columnAt(idx: Int): Column = _invColumnMap(idx)
  def rowAt(idx: Int): Row = _invRowMap(idx)

  def rowKeys: Iterable[Row] = rowMap.keys
  def columnKeys: Iterable[Column] = columnMap.keys

  def orderedRowKeys: Seq[Row] = (0 until rows).map(rowAt(_))
  def orderedColKeys: Seq[Column] = (0 until columns).map(columnAt(_))

  def obtainRow(row: Row): Int = rowMap(row)
  def obtainColumn(col: Column): Int = columnMap(col)

  def rightAdjoinedColKeys(other: RowColKeys[_, _, Row, Column]): Map[Column, Int] = {
    assert((columnMap.keySet intersect other.columnMap.keySet).isEmpty)
    columnMap ++ other.columnMap.map(x => (x._1, x._2 + columns))
  }

  def selectedRowKeys(rows: Seq[Int]): Map[Row, Int] = Map() ++ rows.map(rowAt(_)).zipWithIndex
  def selectedColumnKeys(columns: Seq[Int]): Map[Column, Int] = Map() ++ columns.map(columnAt(_)).zipWithIndex

  def splitColumnKeys(at: Int): (Map[Column, Int], Map[Column, Int]) = {
    val r1 = columnMap.filter(_._2 < at)
    val r2 = columnMap.filter(_._2 >= at).map(x => (x._1, x._2 - at))
    (r1, r2)
  }

}