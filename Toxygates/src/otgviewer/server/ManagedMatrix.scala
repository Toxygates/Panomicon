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

package otgviewer.server

import t.common.shared.sample.ExpressionValue
import otgviewer.server.rpc.Conversions._
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.Synthetic
import t.db.MatrixDBReader
import otgviewer.shared.OTGSample
import t.db.Sample
import t.db.PExprValue
import t.db.ExprValue
import t.db.MatrixContext
import t.db.BasicExprValue
import t.common.shared.sample.SimpleAnnotation
import t.common.shared.sample.ExprMatrix
import t.viewer.server.EVArray

/**
 * Routines for loading a ManagedMatrix
 * and constructing groups.
 *
 * TODO: avoid mixing the t.db groups and samples with the
 * otgviewer.shared ones
 */
abstract class ManagedMatrixBuilder[E >: Null <: ExprValue](reader: MatrixDBReader[E], val probes: Seq[String]) {

  /**
   * Info corresponding to the matrix being built. Gradually updated.
   */
  protected val info = new ManagedMatrixInfo()

  def build(requestColumns: Seq[Group], sparseRead: Boolean,
    fullLoad: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    loadRawData(requestColumns, reader, sparseRead,
      fullLoad)
  }

  /**
   * Construct the columns representing a particular group (g), from the given
   * raw data. Update info to reflect the changes.
   */
  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): ExprMatrix

  protected def defaultColumns[E <: ExprValue](g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[ExprValue]]): ExprMatrix = {
    // A simple average column
    val tus = treatedAndControl(g)._1
    val treatedIdx = unitIdxs(tus, sortedBarcodes)

    info.addColumn(false, g.toString, "Average of treated samples", false, g, false)
    ExprMatrix.withRows(data.map(vs =>
      EVArray(Seq(javaMean(selectIdx(vs, treatedIdx))))),
      probes,
      List(g.toString))
  }

  def loadRawData(requestColumns: Seq[Group],
    reader: MatrixDBReader[E], sparseRead: Boolean,
    fullLoad: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    val pmap = context.probeMap

    val packedProbes = probes.map(pmap.pack)

    val annotations = probes.map(x => new SimpleAnnotation(x)).toVector

    val parts = requestColumns.map(g => {
      //Remove repeated samples as some other algorithms assume distinct samples
      //Also for efficiency
      val samples =
        (if (fullLoad) g.getSamples.toList else samplesForDisplay(g)).
          toVector.distinct
      val sortedSamples = reader.sortSamples(samples.map(b => Sample(b.id)))
      val data = reader.valuesForSamplesAndProbes(sortedSamples,
        packedProbes, sparseRead, false, true)

      println(g.getUnits()(0).toString())

      val rowLookup = Map() ++ data.map(r => r(0).probe -> r)
      val standardOrder = probes.map(p => rowLookup(p))

      //Note, columnsFor causes mutable state in the MatrixInfo being built
      //to change
      val grouped = columnsFor(g, sortedSamples, standardOrder)

      val ungrouped = ExprMatrix.withRows(standardOrder.map(r => EVArray(r.map(asJava(_)))),
        probes, sortedSamples.map(_.sampleId))
      (grouped, ungrouped)
    })

    //Non-commutative operation, so needs reduceLeft instead of plain reduce
    //(collection is parallel)
    val (rawGroupedMat, rawUngroupedMat) = parts.reduceLeft((p1, p2) => {
      val grouped = p1._1 adjoinRight p2._1
      val newCols = p2._2.columnKeys.toSet -- p1._2.columnKeys
      //account for the fact that samples may be shared between requestColumns
      val ungrouped = p1._2 adjoinRight p2._2.selectNamedColumns(newCols.toSeq)
      (grouped, ungrouped)
    })

    new ManagedMatrix(probes, info,
      rawUngroupedMat.copyWithAnnotations(annotations),
      rawGroupedMat.copyWithAnnotations(annotations))
  }

  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean[E <: ExprValue](data: Iterable[E], presentOnly: Boolean = true) = {
    val mean = presentOnly match {
      case true => ExprValue.presentMean(data, "")
      case _    => ExprValue.allMean(data, "")
    }
    var tooltip = data.take(10).map(_.toString).mkString(" ")
    if (data.size > 10) {
      tooltip += ", ..."
    }
    new ExpressionValue(mean.value, mean.call, tooltip)
  }

  final protected def log2(value: ExpressionValue) = {
    new ExpressionValue(Math.log(value.getValue) / Math.log(2), value.getCall, value.getTooltip)
  }

  protected def unitIdxs(us: Iterable[t.viewer.shared.Unit], samples: Seq[Sample]): Seq[Int] = {
    val ids = us.flatMap(u => u.getSamples.map(_.id)).toSet
    val inSet = samples.map(s => ids.contains(s.sampleId))
    inSet.zipWithIndex.filter(_._1).map(_._2)
  }

  protected def samplesForDisplay(g: Group): Iterable[OTGSample] = {
    //TODO
    val (tus, cus) = treatedAndControl(g)
    if (tus.size > 1) {
      //treated samples only
      tus.flatMap(_.getSamples())
    } else {
      //all samples
      g.getSamples()
    }
  }

  //TODO use schema
  protected def treatedAndControl(g: Group) =
    g.getUnits().partition(_.get("dose_level") != "Control")
}

/**
 * No extra columns. Simple averaged fold values.
 */
class FoldBuilder(reader: MatrixDBReader[ExprValue], probes: Seq[String])
    extends ManagedMatrixBuilder[ExprValue](reader, probes) {
  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[ExprValue]]): ExprMatrix =
    defaultColumns(g, sortedBarcodes, data)
}

trait TreatedControlBuilder[E >: Null <: ExprValue] {
  this: ManagedMatrixBuilder[E] =>
  def enhancedColumns: Boolean

  protected def buildRow(raw: Seq[E],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): EVArray

  protected def addColumnInfo(g: Group): Unit
  def colNames(g: Group): Seq[String]

  protected def columnsFor(g: Group, sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): ExprMatrix = {
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
      addColumnInfo(g)

      ExprMatrix.withRows(rows, probes, colNames(g))
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
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): EVArray =
    EVArray(Seq(
      javaMean(selectIdx(raw, treatedIdx)),
      javaMean(selectIdx(raw, controlIdx))))

  protected def addColumnInfo(g: Group) {
    info.addColumn(false, colNames(g)(0), "Average of treated samples", false, g, false)
    info.addColumn(false, colNames(g)(1), "Average of control samples", false, g, false)
  }

  def colNames(g: Group): Seq[String] =
    List(g.toString, g.toString + "(cont)")

}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes)
    with TreatedControlBuilder[PExprValue] {

  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): EVArray = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head
    val fold = log2(javaMean(treatedVs, false))
    EVArray(Seq(fold, new ExpressionValue(first.p, fold.call)))
  }

  protected def addColumnInfo(g: Group) {
    info.addColumn(false, colNames(g)(0), "Average of treated samples", false, g, false)
    info.addColumn(false, colNames(g)(1), "p-values of treated against control", true, g, true)
  }

  def colNames(g: Group) =
    List(g.toString, g.toString + "(p)")
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
    //TODO visibility of these 3 vars
    var currentInfo: ManagedMatrixInfo, var rawUngroupedMat: ExprMatrix,
    var rawGroupedMat: ExprMatrix) {

  protected var currentMat: ExprMatrix = rawGroupedMat

  protected var _synthetics: Vector[Synthetic] = Vector()
  protected var _sortColumn: Option[Int] = None
  protected var _sortAscending: Boolean = false
  protected var _sortAuxTable: Option[ExprMatrix] = None

  protected var requestProbes: Seq[String] = initProbes

  currentInfo.setNumRows(currentMat.rows)

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
  def setFilterThreshold(col: Int, threshold: java.lang.Double): Unit = {
    currentInfo.setColumnFilter(col, threshold)
    resetSortAndFilter()
    filterAndSort()
  }

  /**
   * Select only the rows corresponding to the given probes.
   */
  def selectProbes(probes: Array[String]): Unit = {
    requestProbes = probes
    resetSortAndFilter()
    filterAndSort()
  }

  def probesForAuxTable: Seq[String] = rawGroupedMat.orderedRowKeys

  protected def filterAndSort(): Unit = {
    def f(r: Seq[ExpressionValue]): Boolean = {
      for (
        col <- 0 until currentInfo.numColumns();
        thresh = currentInfo.columnFilter(col);
        if (thresh != null)
      ) {
        val isUpper = currentInfo.isUpperFiltering(col)
        val pass: Boolean = (if (isUpper) {
          Math.abs(r(col).value) <= thresh
        } else {
          Math.abs(r(col).value) >= thresh
        })
        if (!(pass && !java.lang.Double.isNaN(r(col).value))) {
          return false
        }
      }
      true
    }

    currentMat = currentMat.selectNamedRows(requestProbes)
    currentMat = currentMat.filterRows(f)

    currentInfo.setNumRows(currentMat.rows)
    (_sortColumn, _sortAuxTable) match {
      case (Some(sc), _) => sort(sc, _sortAscending)
      case (_, Some(sat)) =>
        sortWithAuxTable(sat, _sortAscending)
      case (None, None) =>
        throw new Exception("Insufficient sort parameters")
    }
  }

  private def sortData(col: Int, ascending: Boolean)(v1: EVArray, v2: EVArray): Boolean = {
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
    currentMat = currentMat.sortRows(sortData(col, ascending))
  }

  /**
   * Adjoin a temporary table consisting of the same rows and one column.
   * Sort everything by that column, then discard the temporary table.
   */
  def sortWithAuxTable(adj: ExprMatrix, ascending: Boolean): Unit = {
    _sortColumn = None
    _sortAscending = ascending
    _sortAuxTable = Some(adj)
    val col = currentMat.columns
    val sortMat = adj.selectNamedRows(currentMat.orderedRowKeys)
    currentMat =
      currentMat.modifyJointly(sortMat, _.sortRows(sortData(col, ascending)))._1
  }

  def addSynthetic(s: Synthetic): Unit = {
    _synthetics :+= s
    addOneSynthetic(s)
  }

  def removeSynthetics(): Unit = {
    _synthetics = Vector()
    val dataColumns = 0 until currentInfo.numDataColumns()
    currentMat = currentMat.selectColumns(dataColumns)
    rawGroupedMat = rawGroupedMat.selectColumns(dataColumns)
    currentInfo.removeSynthetics()
  }

  /**
   * Adds one two-group test to the current matrix.
   */
  protected def addOneSynthetic(s: Synthetic): Unit = {
    s match {
      case test: Synthetic.TwoGroupSynthetic =>
        //TODO
        val g1s = test.getGroup1.getSamples.filter(_.get("dose_level") != "Control").map(_.id)
        val g2s = test.getGroup2.getSamples.filter(_.get("dose_level") != "Control").map(_.id)
        var upper = true

        val currentRows = (0 until currentMat.rows).map(i => currentMat.rowAt(i))
        //Need this to take into account sorting and filtering of currentMat
        val rawData = rawUngroupedMat.selectNamedRows(currentRows)

        currentMat = test match {
          case ut: Synthetic.UTest =>
            currentMat.appendUTest(rawData, g1s, g2s, ut.getShortTitle(null)) //TODO don't pass null
          case tt: Synthetic.TTest =>
            currentMat.appendTTest(rawData, g1s, g2s, tt.getShortTitle(null)) //TODO
          case md: Synthetic.MeanDifference =>
            upper = false
            currentMat.appendDiffTest(rawData, g1s, g2s, md.getShortTitle(null)) //TODO
          case _ => throw new Exception("Unexpected test type!")
        }
        currentInfo.addColumn(true, test.getShortTitle(null), test.getTooltip(), upper, null, false) //TODO
      case _ => throw new Exception("Unexpected test type")
    }
  }

  protected def applySynthetics(): Unit = {
    for (s <- _synthetics) {
      addOneSynthetic(s)
    }
  }

  /**
   * Reset modifications such as filtering, sorting and probe selection.
   * Synthetics are restored after resetting.
   */
  def resetSortAndFilter(): Unit = {
    currentMat = rawGroupedMat
    currentInfo.setNumRows(currentMat.rows)
    applySynthetics()
  }

  /**
   * Obtain the current info for this matrix.
   * The only info members that can change once a matrix has been constructed
   * is data relating to the synthetic columns (since they can be manually
   * added and removed).
   */
  def info: ManagedMatrixInfo = currentInfo

  /**
   * Obtain the current view of this matrix, with all modifications
   * applied.
   */
  def current: ExprMatrix = currentMat

  def rawData: ExprMatrix = rawUngroupedMat
}
