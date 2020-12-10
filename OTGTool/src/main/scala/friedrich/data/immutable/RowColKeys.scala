/**
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Part of the Friedrich bioinformatics framework.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */

package friedrich.data.immutable

import scala.reflect.ClassTag

/**
 * An immutable row/col map that can be manipulated
 * by creating new copies with changes.
 */
trait RowColKeys[V <: Seq[_], Row, Column] {
  this: AbstractMatrix[_, V] =>

  /**
   * Sorted column keys corresponding to each column
   */
  val columnKeys: Array[Column]

  /**
   * Sorted row keys corresponding to each row
   */
  val rowKeys: Array[Row]

  lazy val columnMap: Map[Column, Int] = Map.empty ++ columnKeys.iterator.zipWithIndex.map(x => x._1 -> x._2)
  lazy val rowMap: Map[Row, Int] = Map.empty ++ rowKeys.iterator.zipWithIndex.map(x => x._1 -> x._2)

  def rightAdjoinedColKeys(other: RowColKeys[_, Row, Column])(implicit tag1: ClassTag[Row], tag2: ClassTag[Column]): Array[Column] = {
    assert((columnKeys.toSet intersect other.columnKeys.toSet).isEmpty)
    (columnKeys ++ other.columnKeys).toArray
  }

  def selectedRowKeys(rows: Seq[Int])(implicit tag: ClassTag[Row]): Array[Row] =
    rows.map(rowKeys(_)).toArray
  def selectedColumnKeys(columns: Seq[Int])(implicit tag: ClassTag[Column]): Array[Column] =
    columns.map(columnKeys(_)).toArray

  def row(r: Row): V = row(rowMap(r))
  def column(c: Column): V = column(columnMap(c))
}
