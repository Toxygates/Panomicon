/**
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Part of the Friedrich bioinformatics framework.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */

package friedrich.data.immutable

import scala.collection.SeqLike
import scala.collection.generic.CanBuildFrom

/**
 * A matrix of data that can be represented and modified as row or column vectors.
 * @tparam Self
 * @tparam T Cell type
 * @tparam V Vector type
 */
trait AbstractMatrix[T, V <: Seq[T]] {

  type Self <: AbstractMatrix[T, V]

  def rows: Int

  def columns: Int

  /**
   *  Extract a single row
   */
  def row(row: Int): V

  /**
   * Extract a single column
   */
  def column(col: Int): V

  /**
   * A vector of rows.
   */
  def toRowVectors: Seq[V] = (0 until rows).map(row)

  /**
   * A vector of columns.
   */
  def toColVectors: Seq[V] = (0 until columns).map(column)

  def copyWith(rows: Seq[Seq[T]]): Self
  def copyWithColumns(columns: Seq[V]) = {
    // Transpose the columns into rows
    if (columns.isEmpty) {
      copyWith(Seq())
    } else {
      copyWith(
        columns(0).indices.map(
          r => makeVector(columns.map(c => c(r)))))
    }
  }

  /**
   * Adjoin another matrix to this one on the right hand side.
   * The two matrices must have the same number of rows
   * and be sorted in the same way.
   */
  def adjoinRight(other: Self): Self
  def appendColumn(col: Iterable[T]): Self

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
    mapRows(r => makeVector(r.map(f)))

  def makeVector(s: Seq[T]): V

}

abstract class DataMatrix[T, V <: IndexedSeq[T]](val rowData: IndexedSeq[V], val rows: Int, val columns: Int)
  extends AbstractMatrix[T, V] {

  type Self <: DataMatrix[T, V]

  def apply(row: Int, col: Int): T = rowData(row)(col)

  def row(x: Int): V = rowData(x)
  def column(x: Int): V = makeVector(rowData.map(_(x)))

  def appendColumn(col: Iterable[T]): Self = copyWith(rowData.zip(col).map(x => makeVector(x._1 :+ x._2)))

  def adjoinRight(other: Self): Self = {
    val nrows = (0 until rows).map(i => makeVector(rowData(i) ++ other.row(i)))
    copyWith(nrows)
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
 * Type parameters:
 * T: element,
 * V: vectors,
 * Row: row keys,
 * Column: column keys
 */
abstract class KeyedDataMatrix[T, V <: IndexedSeq[T], Row, Column]
(rowData: IndexedSeq[V], rows: Int, columns: Int, val rowMap: Map[Row, Int], val columnMap: Map[Column, Int])
  extends DataMatrix[T, V](rowData, rows, columns)
  with RowColKeys[V, Row, Column] {

  type Self <: KeyedDataMatrix[T, V, Row, Column]

  def apply(row: Row, col: Column): T = apply(rowMap(row), columnMap(col))

  def copyWith(rows: Seq[Seq[T]]): Self = copyWith(rows, rowMap, columnMap)
  def copyWith(rows: Seq[Seq[T]], rowMap: Map[Row, Int], columnMap: Map[Column, Int]): Self

  def copyWithRowKeys(keys: Map[Row, Int]): Self = copyWith(rowData, keys, columnMap)
  def copyWithColKeys(keys: Map[Column, Int]): Self = copyWith(rowData, rowMap, keys)

  override def adjoinRight(other: Self): Self = {
    val rows = super.adjoinRight(other).toRowVectors
    other match {
      case ra: RowColKeys[V, Row, Column] => {
        copyWith(rows, rowMap, rightAdjoinedColKeys(ra))
      }
      case _ => ???
    }
  }

  override def selectRows(rows: Seq[Int]): Self =
    copyWith(super.selectRows(rows).toRowVectors, selectedRowKeys(rows), columnMap)

  /**
   * NB this allows for permutation as well as selection. Columns are returned
   * in the order requested.
   */
  def selectNamedRows(rows: Seq[Row]): Self =
    selectRows(rows.flatMap(rowMap.get(_)))

  override def selectColumns(columns: Seq[Int]): Self =
    copyWith(super.selectColumns(columns).toRowVectors, rowMap, selectedColumnKeys(columns))

  /**
   * NB this allows for permutation as well as selection. Columns are returned
   * in the order requested.
   */
  def selectNamedColumns(columns: Seq[Column]): Self =
    selectColumns(columns.flatMap(columnMap.get(_)))

  /**
   * Append a column, also registering it by its key
   */
  def appendColumn(col: Iterable[T], key: Column): Self = {
    copyWith(appendColumn(col).toRowVectors, rowMap,
      columnMap + (key -> columns))
  }

  override def filterRows(f: V => Boolean): Self = {
    val remaining = (0 until rows).filter(r => f(row(r)))
    selectRows(remaining)
  }
}
