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

package t.viewer.server

import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Group
import t.common.shared.sample.{ Sample => SSample }
import t.common.shared.sample.{ Unit => TUnit }
import t.db.BasicExprValue
import t.db.ExprValue
import t.db.MatrixContext
import t.db.MatrixDBReader
import t.db.PExprValue
import t.db.Sample
import t.viewer.server.Conversions._
import t.viewer.shared.ColumnFilter
import t.viewer.shared.ManagedMatrixInfo
import t.viewer.shared.Synthetic

/**
 * Routines for loading a ManagedMatrix
 * and constructing groups.
 *
 * TODO: avoid mixing the t.db groups and samples with the
 * otgviewer.shared ones
 */
abstract class ManagedMatrixBuilder[E >: Null <: ExprValue](reader: MatrixDBReader[E], val probes: Seq[String]) {

  def build(requestColumns: Seq[Group], sparseRead: Boolean,
    fullLoad: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    loadRawData(requestColumns, reader, sparseRead,
      fullLoad)
  }

  /**
   * Construct the columns representing a particular group (g), from the given
   * raw data. Update info to reflect the changes.
   * Resulting data should be row-major.
   */
  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): (Seq[Seq[ExprValue]], ManagedMatrixInfo)

  protected def defaultColumns[E <: ExprValue](g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[ExprValue]]): (Seq[Seq[ExprValue]], ManagedMatrixInfo) = {
    // A simple average column
    val tus = treatedAndControl(g)._1
    val treatedIdx = unitIdxs(tus, sortedBarcodes)
    val samples = TUnit.collectBarcodes(tus)

    val info = new ManagedMatrixInfo()
    info.addColumn(false, g.toString, g.toString + ": average of treated samples",
        ColumnFilter.emptyAbsGT, g,
        false, samples)
    val d = data.map(vs => Seq(mean(selectIdx(vs, treatedIdx))))

    (d, info)
  }

  def loadRawData(requestColumns: Seq[Group],
    reader: MatrixDBReader[E], sparseRead: Boolean,
    fullLoad: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    val packedProbes = probes.map(context.probeMap.pack)

    val samples = requestColumns.flatMap(g =>
      (if (fullLoad) g.getSamples else samplesToLoad(g)).
          toVector).distinct
    val sortedSamples = reader.sortSamples(samples.map(b => Sample(b.id)))
    val data = reader.valuesForSamplesAndProbes(sortedSamples,
        packedProbes, sparseRead, false)

    val sortedProbes = data.map(row => row(0).probe)
    val annotations = sortedProbes.map(x => new SimpleAnnotation(x)).toVector

    val cols = requestColumns.par.map(g => {
        println(g.getUnits()(0).toString())
        columnsFor(g, sortedSamples, data)
    }).seq

    //TODO try to avoid adjoining lots of small matrices
    val (groupedData, info) = cols.par.reduceLeft((p1, p2) => {
      val d = (p1._1 zip p2._1).map(r => r._1 ++ r._2)
      val info = p1._2.addAllNonSynthetic(p2._2)
      (d, info)
    })
    val colNames = (0 until info.numColumns()).map(i => info.columnName(i))
    val grouped = ExprMatrix.withRows(groupedData, sortedProbes, colNames)

    //TODO: avoid EVArray building
    val ungrouped = ExprMatrix.withRows(data,
        sortedProbes, sortedSamples.map(_.sampleId))

    val baseColumns = Map() ++ (0 until info.numDataColumns()).map(i => {
      val sampleIds = info.samples(i).map(_.id).toSeq
      val sampleIdxs = sampleIds.map(i => ungrouped.columnMap.get(i)).flatten
      (i -> sampleIdxs)
    })

    new ManagedMatrix(sortedProbes, info,
      ungrouped.copyWithAnnotations(annotations),
      grouped.copyWithAnnotations(annotations),
      baseColumns,
      log2transform)
  }

  protected def log2transform: Boolean = false

  protected def mean(data: Iterable[ExprValue], presentOnly: Boolean = true) = {
    if (presentOnly) {
      ExprValue.presentMean(data, "")
    } else {
      ExprValue.allMean(data, "")
    }
  }

  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean[E <: ExprValue](data: Iterable[E], presentOnly: Boolean = true) = {
    val m = mean(data, presentOnly)
    new ExpressionValue(m.value, m.call, null)
  }

  protected def unitIdxs(us: Iterable[t.common.shared.sample.Unit], samples: Seq[Sample]): Seq[Int] = {
    val ids = us.flatMap(u => u.getSamples.map(_.id)).toSet
    val inSet = samples.map(s => ids.contains(s.sampleId))
    inSet.zipWithIndex.filter(_._1).map(_._2)
  }

  protected def samplesToLoad(g: Group): Array[SSample] = {
    val (tus, cus) = treatedAndControl(g)
    tus.flatMap(_.getSamples())
  }

  //TODO use schema
  protected def treatedAndControl(g: Group) =
    g.getUnits().partition(_.get("dose_level") != "Control")
}

trait TreatedControlBuilder[E >: Null <: ExprValue] {
  this: ManagedMatrixBuilder[E] =>
  def enhancedColumns: Boolean

  protected def buildRow(raw: Seq[E],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): Seq[ExprValue]

  protected def columnInfo(g: Group): ManagedMatrixInfo
  def colNames(g: Group): Seq[String]

  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): (Seq[Seq[ExprValue]], ManagedMatrixInfo) = {
    //TODO
    val (tus, cus) = treatedAndControl(g)
    println(s"#Control units: ${cus.size} #Non-control units: ${tus.size}")

    if (tus.size > 1 || (!enhancedColumns) || cus.size == 0 || tus.size == 0) {
      // A simple average column
      defaultColumns(g, sortedBarcodes, data)
    } else if (tus.size == 1) {
      // Possibly insert a control column as well as the usual one

      val ti = unitIdxs(tus, sortedBarcodes)
      val ci = unitIdxs(cus, sortedBarcodes)

      val rows = data.map(vs => buildRow(vs, ti, ci))
      val i = columnInfo(g)

      (rows, i)
    } else {
      throw new Exception("No units in group")
    }
  }
}

/**
 * Columns consisting of normalized intensity / "absolute value" expression data
 * for both treated and control samples.
 */
class NormalizedBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[ExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[ExprValue](reader, probes)
    with TreatedControlBuilder[ExprValue] {

  protected def buildRow(raw: Seq[ExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): Seq[ExprValue] =
    Seq(
      mean(selectIdx(raw, treatedIdx)),
      mean(selectIdx(raw, controlIdx)))

  protected def columnInfo(g: Group) = {
    val (tus, cus) = treatedAndControl(g)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, colNames(g)(0),
        colNames(g)(0) + ": average of treated samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectBarcodes(tus))
    info.addColumn(false, colNames(g)(1),
        colNames(g)(1) + ": average of control samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectBarcodes(cus))
    info
  }

  def colNames(g: Group): Seq[String] =
    List(g.toString, g.toString + "(cont)")

  override protected def samplesToLoad(g: Group): Array[SSample] = {
    val (tus, cus) = treatedAndControl(g)
    if (tus.size > 1) {
      super.samplesToLoad(g)
    } else {
      //all samples
      g.getSamples()
    }
  }
}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes)
    with TreatedControlBuilder[PExprValue] {

  import ManagedMatrix._

  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): Seq[ExprValue] = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head
    val fold = log2(javaMean(treatedVs, false))
    Seq(fold, new BasicExprValue(first.p, fold.call))
  }

  override protected def log2transform = true

  override protected def columnInfo(g: Group): ManagedMatrixInfo = {
    val tus = treatedAndControl(g)._1
    val samples = TUnit.collectBarcodes(tus)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, colNames(g)(0),
        colNames(g)(0) + ": average of treated samples",
        ColumnFilter.emptyAbsGT, g, false, samples)
    info.addColumn(false, colNames(g)(1),
        colNames(g)(1) + ": p-values of treated against control",
        ColumnFilter.emptyLT, g, true,
        Array[SSample]())
    info
  }

  def colNames(g: Group) =
    List(g.toString, g.toString + "(p)")
}

object ManagedMatrix {

  private val l2 = Math.log(2)

  def log2(value: ExpressionValue) = {
    new ExpressionValue(Math.log(value.getValue) / l2, value.getCall, value.getTooltip)
  }

  def log2[E <: ExprValue](value: E): ExprValue = {
    ExprValue.apply(Math.log(value.value) / l2, value.call, value.probe)
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
    def f(r: Seq[ExprValue]): Boolean = {
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

  private def sortData(col: Int, ascending: Boolean)(v1: Seq[ExprValue], v2: Seq[ExprValue]): Boolean = {
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
    current = current.sortRows(sortData(col, ascending))
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
      current.modifyJointly(sortMat, _.sortRows(sortData(col, ascending)))._1
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
        val g1s = test.getGroup1.getSamples.filter(_.get("dose_level") != "Control").map(_.id)
        val g2s = test.getGroup2.getSamples.filter(_.get("dose_level") != "Control").map(_.id)

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
  def finalTransform(m: ExprMatrix): ExprMatrix = {
    if (log2Transform) {
      m.map(ManagedMatrix.log2)
    } else {
      m
    }
  }

}
