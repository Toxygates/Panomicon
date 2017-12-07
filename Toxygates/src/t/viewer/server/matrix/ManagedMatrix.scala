/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.viewer.server.matrix

import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Group
import t.common.shared.sample.{ Sample => SSample }
import t.common.shared.sample.{ Unit => TUnit }
import t.db._
import t.viewer.server.Conversions._
import t.viewer.shared.ColumnFilter
import t.viewer.shared.ManagedMatrixInfo
import t.viewer.shared.Synthetic
import otg.model.sample.OTGAttribute


object ManagedMatrix {
 type RowData = Seq[ExprValue]
 
  final private val l2 = Math.log(2)
  final private def log2(v: Double): Double = Math.log(v) / l2

  def log2(value: ExpressionValue): ExpressionValue = {
    new ExpressionValue(log2(value.getValue), value.getCall, value.getTooltip)
  }

  def log2[E <: ExprValue](value: E): ExprValue = {
    ExprValue.apply(log2(value.value), value.call, value.probe)
  }

  import java.lang.{Double => JDouble}
  def makeTooltip[E <: ExprValue](data: Iterable[E]): String = {
    data.toSeq.filter(v => !JDouble.isNaN(v.value)).
      sortWith(ExprValue.isBefore).map(_.toString).mkString(" ")
  }
}

/**
 * A server-side ExprMatrix and support logic for
 * sorting, filtering, data loading etc.
 *
 * A managed matrix is constructed on the basis of some number of
 * "request columns" but may insert additional columns with extra information.
 * The info object should be used to query what columns have actually been
 * constructed.
 *
 * @param rawUngroupedMat ungrouped matrix.
 * Mainly used for computing T- and U-tests. Sorting is irrelevant.
 *
 * @param rawGroupedMat unfiltered matrix.
 *  The final view is obtained by filtering this (if requested).
 */

class ManagedMatrix(val initProbes: Seq[String],
    //TODO visibility of these members
    val currentInfo: ManagedMatrixInfo,
    val rawUngrouped: ExprMatrix,
    var rawGrouped: ExprMatrix,
    val baseColumnMap: Map[Int, Seq[Int]],
    val log2Transform: Boolean = false) {
  
  import ManagedMatrix._

  var current: ExprMatrix = rawGrouped

  protected var _synthetics: Vector[Synthetic] = Vector()
  protected var _sortColumn: Option[Int] = None
  protected var _sortAscending: Boolean = false
  protected var _sortAuxTable: Option[ExprMatrix] = None

  protected var requestProbes: Seq[String] = initProbes

  updateRowInfo()

  /**
   * For the given column in the grouped matrix,
   * which columns in the ungrouped matrix are its basis?
   */
  def baseColumns(col: Int): Seq[Int] = baseColumnMap.get(col).getOrElse(List())

  /**
   * What is the current sort column?
   * Undefined if the last sort operation was done by an aux table.
   */
  def sortColumn: Option[Int] = _sortColumn

  /**
   * Is the current sort type ascending?
   */
  def sortAscending: Boolean = _sortAscending

  /**
   * Set the filtering threshold for a column with separate filtering.
   */
  def setFilter(col: Int, f: ColumnFilter): Unit = {
    currentInfo.setColumnFilter(col, f)
    resetSortAndFilter()
    filterAndSort()
  }

  /**
   * Select only the rows corresponding to the given probes.
   */
  def selectProbes(probes: Seq[String]): Unit = {
    requestProbes = probes
    resetSortAndFilter()
    filterAndSort()
  }

  def probesForAuxTable: Seq[String] = rawGrouped.orderedRowKeys

  protected def filterAndSort(): Unit = {
    def f(r: RowData): Boolean = {
      for (
        col <- 0 until currentInfo.numColumns();
        filt = currentInfo.columnFilter(col);
        if (filt != null && filt.active())
      ) {
        //Note, comparisons with NaN are always false
        val pass = filt.test(r(col).value)
        if (!pass || !r(col).present) {
          return false
        }
      }
      true
    }

    println(s"Filter: ${currentInfo.numDataColumns} data ${currentInfo.numSynthetics} synthetic")

    //TODO avoid selecting here
    current = current.selectNamedRows(requestProbes).filterRows(f)
    (_sortColumn, _sortAuxTable) match {
      case (Some(sc), _) => sort(sc, _sortAscending)
      case (_, Some(sat)) =>
        sortWithAuxTable(sat, _sortAscending)
      case (None, None) =>
        throw new Exception("Insufficient sort parameters")
    }
  }

  private def sortRows(col: Int, ascending: Boolean)(v1: RowData, v2: RowData): Boolean = {
    val ev1 = v1(col)
    val ev2 = v2(col)
    if (ev1.call == 'A' && ev2.call != 'A') {
      false
    } else if (ev1.call != 'A' && ev2.call == 'A') {
      true
    } else {
      //Use this to handle NaN correctly (comparison method MUST be transitive)
      def cmp(x: Double, y: Double) = java.lang.Double.compare(x, y)
      if (ascending) {
        cmp(ev1.value, ev2.value) < 0
      } else {
        cmp(ev1.value, ev2.value) > 0
      }
    }
  }

  def sort(col: Int, ascending: Boolean): Unit = {
    _sortColumn = Some(col)
    _sortAscending = ascending
    current = current.sortRows(sortRows(col, ascending))
    updateRowInfo()
  }

  /**
   * Adjoin a temporary table consisting of the same rows and one column.
   * Sort everything by that column, then discard the temporary table.
   */
  def sortWithAuxTable(adj: ExprMatrix, ascending: Boolean): Unit = {
    _sortColumn = None
    _sortAscending = ascending
    _sortAuxTable = Some(adj)
    val col = current.columns
    val sortMat = adj.selectNamedRows(current.orderedRowKeys)
    current =
      current.modifyJointly(sortMat, _.sortRows(sortRows(col, ascending)))._1
    updateRowInfo()
  }

  def removeSynthetics(): Unit = {
    _synthetics = Vector()
    val dataColumns = 0 until currentInfo.numDataColumns()
    current = current.selectColumns(dataColumns)
    rawGrouped = rawGrouped.selectColumns(dataColumns)
    currentInfo.removeSynthetics()
  }

  def addSynthetic(s: Synthetic): Unit = {
    _synthetics :+= s
    addSyntheticInner(s)
  }

  /**
   * Adds one two-group test to the current matrix.
   */
  protected def addSyntheticInner(s: Synthetic): Unit = {
    s match {
      case test: Synthetic.TwoGroupSynthetic =>
        //TODO
        val g1s = test.getGroup1.getSamples.filter(_.get(OTGAttribute.DoseLevel) != "Control")
          .map(_.id)
        val g2s = test.getGroup2.getSamples.filter(_.get(OTGAttribute.DoseLevel) != "Control")
          .map(_.id)

        val currentRows = (0 until current.rows).map(i => current.rowAt(i))
        //Need this to take into account sorting and filtering of currentMat
        val rawData = finalTransform(rawUngrouped).selectNamedRows(currentRows)

        current = test match {
          case ut: Synthetic.UTest =>
            current.appendUTest(rawData, g1s, g2s, ut.getShortTitle(null)) //TODO don't pass null
          case tt: Synthetic.TTest =>
            current.appendTTest(rawData, g1s, g2s, tt.getShortTitle(null)) //TODO
          case md: Synthetic.MeanDifference =>
            current.appendDiffTest(current, Seq(test.getGroup1.getName),
                Seq(test.getGroup2.getName), md.getShortTitle(null)) //TODO
          case _ => throw new Exception("Unexpected test type!")
        }
        val name = test.getShortTitle(null);
        if (!currentInfo.hasColumn(name)) {
          currentInfo.addColumn(true, name, test.getTooltip(),
            ColumnFilter.emptyLT, null, false,
            Array[SSample]()) //TODO
        }
      case _ => throw new Exception("Unexpected test type")
    }
  }

  protected def reapplySynthetics(): Unit = {
    for (s <- _synthetics) {
      addSyntheticInner(s)
    }
  }

  /**
   * Reset modifications such as filtering, sorting and probe selection.
   * Synthetics are restored after resetting.
   */
  def resetSortAndFilter(): Unit = {
    //drops synthetic columns
    current = rawGrouped
    //note - we keep the synthetic column info (such as filters) in the currentInfo
    updateRowInfo()
    reapplySynthetics()
  }

  private def updateRowInfo() {
    currentInfo.setNumRows(current.rows)
    currentInfo.setAtomicProbes(current.annotations.flatMap(_.atomics).toArray)
  }

  /**
   * Obtain the current info for this matrix.
   * The only info members that can change once a matrix has been constructed
   * is data relating to the synthetic columns (since they can be manually
   * added and removed).
   */
  def info: ManagedMatrixInfo = currentInfo

  /**
   * Obtain a view of a matrix with the log-2 transform
   * potentially applied.
   */
  private[server] def finalTransform(m: ExprMatrix): ExprMatrix = {
    if (log2Transform) {
      m.map(e => ManagedMatrix.log2(e))
    } else {
      m
    }
  }

}
