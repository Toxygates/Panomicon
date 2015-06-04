/**
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Part of the Friedrich bioinformatics framework.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */

package friedrich.data.immutable
import friedrich.data._

/**
 * An immutable row/col allocation.
 */
trait RowColAllocation[T, V <: Seq[T], Row, Column] extends
 friedrich.data.RowColAllocation[T, V, Row, Column] {
  this: AbstractMatrix[_, T, V] =>

  val columnMap: Map[Column, Int]
  val rowMap: Map[Row, Int]
  
  private val _invColumnMap = Map() ++ columnMap.map(x => x._2 -> x._1)
  private val _invRowMap = Map() ++ rowMap.map(x => x._2 -> x._1)
    
  def columnAt(idx: Int): Column = _invColumnMap(idx)
  def rowAt(idx: Int): Row = _invRowMap(idx)

  def rowKeys: Iterable[Row] = rowMap.keys
  def columnKeys: Iterable[Column] = columnMap.keys

  def obtainRow(row: Row): Int = rowMap(row)
  def obtainColumn(col: Column): Int = columnMap(col)
  
  def rightAdjoinedColAlloc(other: RowColAllocation[_, _, Row, Column]): Map[Column, Int] = {
    assert((columnMap.keySet intersect other.columnMap.keySet) == Set())
    columnMap ++ other.columnMap.map(x => (x._1, x._2 + columns))
  }
    
  def selectedRowAlloc(rows: Seq[Int]): Map[Row, Int] =  Map() ++ rows.map(rowAt(_)).zipWithIndex
  def selectedColumnAlloc(columns: Seq[Int]): Map[Column, Int] =  Map() ++ columns.map(columnAt(_)).zipWithIndex
  
  def splitColumnAlloc(at: Int): (Map[Column, Int], Map[Column, Int]) = {
    val r1 = columnMap.filter(_._2 < at)
    val r2 = columnMap.filter(_._2 >= at).map(x => (x._1, x._2 - at))
    (r1, r2)
  }
  
}