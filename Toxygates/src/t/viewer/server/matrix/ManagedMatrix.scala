/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
import t.common.shared.sample.{Sample => SSample}
import t.common.shared.sample.{Unit => TUnit}

object ManagedMatrix {
 type RowData = Seq[ExprValue]

  def log2(value: ExpressionValue): ExpressionValue = {
    new ExpressionValue(ExprValue.log2(value.getValue), value.getCall, value.getTooltip)
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

class CoreMatrix(val initProbes: Seq[String],
    val currentInfo: ManagedMatrixInfo,
    val rawUngrouped: ExprMatrix,
    var rawGrouped: ExprMatrix,
    val baseColumnMap: Map[Int, Seq[Int]],
    val log2Transform: Boolean = false) {

  import ManagedMatrix._

  var current: ExprMatrix = rawGrouped

  protected var _sortColumn: Option[Int] = None
  protected var _sortAscending: Boolean = false

  protected var requestProbes: Seq[String] = initProbes

  updateRowInfo()

  /**
   * For the given column in the grouped matrix,
   * which columns in the ungrouped matrix are its basis?
   */
  def baseColumns(col: Int): Seq[Int] = baseColumnMap.get(col).getOrElse(List())

  /**
   * What is the current sort column?
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
   * Set multiple column filters at once.
   */
  def setFilters(fs: Seq[ColumnFilter]): Unit = {
    for ((f, i) <- fs.zipWithIndex) {
      currentInfo.setColumnFilter(i, f)
    }
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
    _sortColumn match {
      case Some(sc) => sort(sc, _sortAscending)
      case _ => //not sorting
    }
  }

  private final def sortRows(col: Int, ascending: Boolean)(v1: RowData, v2: RowData): Boolean = {
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
   * Reset modifications such as filtering, sorting and probe selection.
   */
  def resetSortAndFilter(): Unit = {
    current = rawGrouped
    updateRowInfo()
  }

  private def updateRowInfo() {
    currentInfo.setNumRows(current.rows)
    currentInfo.setAtomicProbes(current.annotations.flatMap(_.atomics).toArray)
  }

  /**
   * Obtain the current info for this matrix.
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

/**
 * Synthetic column management.
 *
 * The only info members that can change once a matrix has been constructed
 * is data relating to the synthetic columns (since they can be manually
 * added and removed).
 */
trait Synthetics extends CoreMatrix {

  protected var _synthetics: Vector[Synthetic] = Vector()

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
     Option(s.initFilter) match {
      case Some(f) =>
        val col = current.columns - 1
        currentInfo.setColumnFilter(col, f)
        filterAndSort()
      case _ =>
    }
  }

  /**
   * Adds one two-group test to the current matrix.
   */
  protected def addSyntheticInner(s: Synthetic): Unit = {
    s match {
      case test: Synthetic.TwoGroupSynthetic =>
        //TODO avoid using magic strings
        val g1s = test.getGroup1.getSamples.filter(_.get(OTGAttribute.DoseLevel) != "Control")
          .map(_.id)
        val g2s = test.getGroup2.getSamples.filter(_.get(OTGAttribute.DoseLevel) != "Control")
          .map(_.id)

        val currentRows = (0 until current.rows).map(i => current.rowAt(i))
        //Need this to take into account sorting and filtering of currentMat
        val rawData = finalTransform(rawUngrouped).selectNamedRows(currentRows)

        current = test match {
          case ut: Synthetic.UTest =>
            current.appendUTest(rawData, g1s, g2s, ut.getShortTitle)
          case tt: Synthetic.TTest =>
            current.appendTTest(rawData, g1s, g2s, tt.getShortTitle)
          case md: Synthetic.MeanDifference =>
            current.appendDiffTest(current, Seq(test.getGroup1.getName),
                Seq(test.getGroup2.getName), md.getShortTitle)
          case _ => throw new Exception("Unexpected test type!")
        }
        val name = test.getName
        if (!currentInfo.hasColumn(name)) {
          currentInfo.addColumn(true, name, test.getTooltip(),
            ColumnFilter.emptyLT, null, false,
            Array[SSample]())
        }
      case precomp: Synthetic.Precomputed =>
        val name = precomp.getName
        if (!currentInfo.hasColumn(name)) {
          currentInfo.addColumn(true, name, precomp.getTooltip,
            ColumnFilter.emptyGT, null, false,
            Array[SSample]())
        }
        val data = precomp.getData
        val inOrder = (0 until current.rows).map(i => data.get(current.rowAt(i)).toDouble)

        current = current.appendStatic(inOrder, precomp.getName)
      case _ => throw new Exception("Unexpected test type")
    }
  }

  protected def reapplySynthetics(): Unit = {
    for (s <- _synthetics) {
      addSyntheticInner(s)
    }
  }

  override def resetSortAndFilter(): Unit = {
    super.resetSortAndFilter()
    reapplySynthetics()
  }
}

class ManagedMatrix(initProbes: Seq[String],
    currentInfo: ManagedMatrixInfo,
    rawUngrouped: ExprMatrix,
    rawGrouped: ExprMatrix,
    baseColumnMap: Map[Int, Seq[Int]],
    log2Transform: Boolean = false)
    extends CoreMatrix(initProbes, currentInfo, rawUngrouped,
                       rawGrouped, baseColumnMap, log2Transform) with Synthetics
