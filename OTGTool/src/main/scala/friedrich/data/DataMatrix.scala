/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Part of the Friedrich bioinformatics framework.
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */

package friedrich.data

import scala.collection.generic.CanBuildFrom

/**
 * A matrix of 2D data, made up of vectors. T is the element type.
 * V is the underlying vector type.
 * The matrix can be decomposed both as column vectors and as row vectors.
*/
trait DataMatrix[T, V <: Seq[T]] {
  def rows: Int
  
  def columns: Int
  
  def apply(row: Int, col: Int): T
  
  implicit def builder: CanBuildFrom[Seq[T], T, V]
  def fromSeq(s: Seq[T]): V = (builder() ++= s).result
  
  /**
   * A vector of rows.
   */
  def toRowVectors: Seq[V] = (0 until rows).toVector.map(row(_))

  /**
   * A vector of columns.
   */
  def toColVectors: Seq[V] = (0 until columns).toVector.map(column(_))

  /** 
   *  Extract a single row
   */
  def row(row: Int): V
  
  /**
   * Extract a single column
   */
  def column(col: Int): V

  override def toString = "DataMatrix " + rows + "x" + columns + "\n" +
    toRowVectors.map(_.toString + "\n").reduce(_ + _)
}


/**
 * A trait to support keyed columns and rows in 2D data.
 * The Row and Column types are the keys used for lookup 
 * (for example, String can be used to have named columns and rows).
 * Lookup by (integer) indexed position is still possible.
 */
trait RowColAllocation[T, V <: Seq[T], Row, Column] {
  this: DataMatrix[T, V] =>

  def rowKeys: Iterable[Row]
  
  def columnKeys: Iterable[Column]
    
  /**
   * Look up the row key corresponding to the given position
   */
  def rowAt(idx: Int): Row
  
  /**
   * Look up the column key corresponding to the given position
   */
  def columnAt(idx: Int): Column
  
  /**
   * Look up the position corresponding to the given row key
   */
  def obtainRow(row: Row): Int
  
  /**
   * Look up the position corresponding to the given column key
   */
  def obtainColumn(col: Column): Int
  
  /**
   * Extract the element addressed by the given row and column keys
   */
  def apply(row: Row, col: Column): T = apply(obtainRow(row), obtainColumn(col))
  
  def row(r: Row): V = row(obtainRow(r))
  
  def column(c: Column): V = column(obtainColumn(c))
}

