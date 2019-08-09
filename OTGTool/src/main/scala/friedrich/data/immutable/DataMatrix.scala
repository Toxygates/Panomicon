/**
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Part of the Friedrich bioinformatics framework.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */

package friedrich.data.immutable

import scala.collection.SeqLike
import scala.collection.generic.CanBuildFrom

trait AbstractMatrix[Self <: AbstractMatrix[Self, T, V], T, V <: Seq[T]]
  extends friedrich.data.DataMatrix[T, V] {

  def copyWith(other: Self): Self = copyWith(other.toRowVectors)
  def copyWith(rows: Seq[Seq[T]]): Self
  def copyWithColumns(columns: Seq[V]) = {
    // Transpose the columns into rows
    if (columns.isEmpty) {
      copyWith(Seq())
    } else {
      copyWith(
        (0 until columns(0).size).map(
          r => fromSeq(columns.map(c => c(r)))))
    }
  }

  def updated(row: Int, col: Int, v: T): Self

  /**
   * Adjoin another matrix to this one on the right hand side.
   * The two matrices must have the same number of rows
   * and be sorted in the same way.
   */
  def adjoinRight(other: Self): Self
  def appendColumn(col: Iterable[T]): Self
  def modifyJointly(other: Self, f: (Self) => Self): (Self, Self)

  /**
   * Split this matrix vertically so that the first column in the second matrix
   * has the given offset in this matrix.
   * Example: if at is 3, then the first 3 columns will be returned in the first matrix,
   * and the remaining ones in the second matrix.
   */
  def verticalSplit(at: Int): (Self, Self)

  /**
   * Select rows by index, optionally rearranging them in a different order, returning them
   * in a new matrix
   */
  def selectRows(rows: Seq[Int]): Self

  /**
   * Select columns by index, optionally rearranging them in a different order, returning them
   * in a new matrix
   */
  def selectColumns(cols: Seq[Int]): Self
  def filterRows(f: V => Boolean): Self =
    copyWith(toRowVectors.filter(f))

  def sortRows(f: (V, V) => Boolean): Self

  def mapRows(f: V => V): Self =
    copyWith(toRowVectors.map(f))

  def map(f: T => T): Self =
    mapRows(r => fromSeq(r.map(f)))
}

abstract class DataMatrix[Self <: DataMatrix[Self, T, V], T, V <: IndexedSeq[T]](val data: IndexedSeq[V], val rows: Int, val columns: Int)
  extends AbstractMatrix[Self, T, V] {

  def apply(row: Int, col: Int) = data(row)(col)
  def updated(row: Int, col: Int, value: T) = copyWith(data.updated(row, fromSeq(data(row).updated(col, value))))

  def row(x: Int): V = data(x)
  def column(x: Int): V = fromSeq(data.map(_(x)))

  def appendColumn(col: Iterable[T]): Self = copyWith(data.zip(col).map(x => fromSeq(x._1 :+ x._2)))

  def adjoinRight(other: Self): Self = {
    val nrows = (0 until rows).map(i => fromSeq(data(i) ++ other.row(i)))
    copyWith(nrows)
  }

  def verticalSplit(at: Int): (Self, Self) = {
    val r1 = copyWith(data.map(r => fromSeq(r.take(at))))
    val r2 = copyWith(data.map(r => fromSeq(r.drop(at))))
    (r1, r2)
  }

  /**
   * Adjoin this matrix to another. Then perform some update function on the resulting matrix.
   * Finally split the matrices again, at the same position.
   * The update function should preserve the dimensions of the matrices.
   */
  def modifyJointly(other: Self, f: (Self) => Self): (Self, Self) = {
    f(adjoinRight(other)).verticalSplit(columns)
  }

  def selectRows(rows: Seq[Int]): Self = copyWith(rows.map(row(_)))

  def selectColumns(columns: Seq[Int]): Self =
    copyWithColumns(columns.map(column(_)))

  def sortRows(f: (V, V) => Boolean): Self = {
    val ixs = (0 until rows)
    val z = ixs.zip(ixs.map(row(_)))
    val sorted = z.sortWith((x, y) => f(x._2, y._2))
    val sortedIdx = sorted.map(_._1)
    selectRows(sortedIdx)
  }

}

/**
 * A vector (here, some seq type) backed data matrix that also has keyed rows and columns.
 *
 * Type parameters: Self is the matrix selftype,
 * T: element,
 * V: vectors,
 * Row: row keys,
 * Column: column keys
 */
abstract class KeyedDataMatrix[Self <: KeyedDataMatrix[Self, T, V, Row, Column], T, V <: IndexedSeq[T], Row, Column]
(data: IndexedSeq[V], rows: Int, columns: Int, val rowMap: Map[Row, Int], val columnMap: Map[Column, Int])
  extends DataMatrix[Self, T, V](data, rows, columns)
  with RowColKeys[T, V, Row, Column] {

  def copyWith(rows: Seq[Seq[T]]): Self = copyWith(rows, rowMap, columnMap)
  def copyWith(rows: Seq[Seq[T]], rowMap: Map[Row, Int], columnMap: Map[Column, Int]): Self

  def copyWithRowKeys(keys: Map[Row, Int]): Self = copyWith(data, keys, columnMap)
  def copyWithColKeys(keys: Map[Column, Int]): Self = copyWith(data, rowMap, keys)

  override def adjoinRight(other: Self): Self = {
    val r = super.adjoinRight(other)
    other match {
      case ra: RowColKeys[T, V, Row, Column] => {
        r.copyWithColKeys(rightAdjoinedColKeys(ra))
      }
      case _ => r //what's the right behaviour?
    }
  }

  override def verticalSplit(at: Int): (Self, Self) = {
    val (r1, r2) = super.verticalSplit(at)
    val (ca1, ca2) = splitColumnKeys(at)

    val rr1 = r1.copyWithColKeys(ca1)
    val rr2 = r2.copyWithColKeys(ca2)
    (rr1, rr2)
  }

  override def selectRows(rows: Seq[Int]): Self =
    super.selectRows(rows).copyWithRowKeys(selectedRowKeys(rows))

  /**
   * NB this allows for permutation as well as selection. Columns are returned
   * in the order requested.
   */
  def selectNamedRows(rows: Seq[Row]): Self = selectRows(rows.flatMap(rowMap.get(_)))

  override def selectColumns(columns: Seq[Int]): Self =
    super.selectColumns(columns).copyWithColKeys(selectedColumnKeys(columns))

  /**
   * NB this allows for permutation as well as selection. Columns are returned
   * in the order requested.
   */
  def selectNamedColumns(columns: Seq[Column]): Self = selectColumns(columns.flatMap(columnMap.get(_)))

  /**
   * Append a column, also registering it by its key
   */
  def appendColumn(col: Iterable[T], key: Column): Self = {
    val r = appendColumn(col)
    r.copyWithColKeys(columnMap + (key -> columns))
  }

  override def filterRows(f: V => Boolean): Self = {
    val remaining = (0 until rows).filter(r => f(row(r)))
    selectRows(remaining)
  }
}
