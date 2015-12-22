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
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.Synthetic
import t.db.MatrixDBReader
import t.common.shared.sample.{Sample => SSample, Unit => TUnit}
import t.common.shared.sample.ExprMatrix
import t.common.shared.sample.Group
import t.common.shared.sample.SimpleAnnotation
import t.db.Sample
import t.db.PExprValue
import t.db.ExprValue
import t.db.MatrixContext
import t.db.BasicExprValue
import t.common.shared.sample.EVArray

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
   */
  protected def columnsFor(g: Group, probes: Seq[String], sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): (ExprMatrix, ManagedMatrixInfo)

  protected def defaultColumns[E <: ExprValue](g: Group, probes: Seq[String], sortedBarcodes: Seq[Sample],
    data: Seq[Seq[ExprValue]]): (ExprMatrix, ManagedMatrixInfo) = {
    // A simple average column
    val tus = treatedAndControl(g)._1
    val treatedIdx = unitIdxs(tus, sortedBarcodes)
    val samples = TUnit.collectBarcodes(tus)

    val info = new ManagedMatrixInfo()
    info.addColumn(false, g.toString, g.toString + ": average of treated samples", false, g,
        false, samples)
    val e = ExprMatrix.withRows(data.map(vs =>
      Seq(mean(selectIdx(vs, treatedIdx)))),
      probes,
      List(g.toString))
    (e, info)
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
        //TODO avoid EVArray building
        columnsFor(g, sortedProbes, sortedSamples, data)
    }).seq

    //TODO try to avoid adjoining lots of small matrices
    val (grouped, info) = cols.par.reduceLeft((p1, p2) => {
      val grouped = p1._1 adjoinRight p2._1
      val info = p1._2.addAllNonSynthetic(p2._2)
      (grouped, info)
    })

    //TODO: avoid EVArray building
    val ungrouped = ExprMatrix.withRows(data,
        sortedProbes, sortedSamples.map(_.sampleId))

    val bmap = Map() ++ (0 until requestColumns.size).map(i => {
      val cg = info.columnGroup(i)
      val sampleIds = cg.samples.map(_.id).toSeq
      val sampleIdxs = sampleIds.map(i => ungrouped.columnMap.get(i)).flatten
      (i -> sampleIdxs)
    })

    new ManagedMatrix(sortedProbes, info,
      ungrouped.copyWithAnnotations(annotations),
      grouped.copyWithAnnotations(annotations),
      bmap,
      log2tooltips)
  }

  protected def log2tooltips: Boolean = false

  protected def mean(data: Iterable[ExprValue], presentOnly: Boolean = true) = {
    if (presentOnly) {
      ExprValue.presentMean(data, "")
    } else {
      ExprValue.allMean(data, "")
    }
  }

  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean[E <: ExprValue](data: Iterable[E], presentOnly: Boolean = true) = {
    val mean = presentOnly match {
      case true => ExprValue.presentMean(data, "")
      case _    => ExprValue.allMean(data, "")
    }

    new ExpressionValue(mean.value, mean.call, null) // makeTooltip(data))
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

/**
 * No extra columns. Simple averaged fold values.
 */
class FoldBuilder(reader: MatrixDBReader[ExprValue], probes: Seq[String])
    extends ManagedMatrixBuilder[ExprValue](reader, probes) {

  protected def columnsFor(g: Group, pobes: Seq[String], sortedBarcodes: Seq[Sample],
    data: Seq[Seq[ExprValue]]): (ExprMatrix, ManagedMatrixInfo) =
    defaultColumns(g, probes, sortedBarcodes, data)

}

trait TreatedControlBuilder[E >: Null <: ExprValue] {
  this: ManagedMatrixBuilder[E] =>
  def enhancedColumns: Boolean

  protected def buildRow(raw: Seq[E],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): Seq[ExprValue]

  protected def columnInfo(g: Group): ManagedMatrixInfo
  def colNames(g: Group): Seq[String]

  protected def columnsFor(g: Group, probes: Seq[String], sortedBarcodes: Seq[Sample],
    data: Seq[Seq[E]]): (ExprMatrix, ManagedMatrixInfo) = {
    //TODO
    val (tus, cus) = treatedAndControl(g)
    println(s"#Control units: ${cus.size} #Non-control units: ${tus.size}")

    if (tus.size > 1 || (!enhancedColumns) || cus.size == 0 || tus.size == 0) {
      // A simple average column
      defaultColumns(g, probes, sortedBarcodes, data)
    } else if (tus.size == 1) {
      // Possibly insert a control column as well as the usual one

      val ti = unitIdxs(tus, sortedBarcodes)
      val ci = unitIdxs(cus, sortedBarcodes)

      val rows = data.map(vs => buildRow(vs, ti, ci))
      val i = columnInfo(g)

      (ExprMatrix.withRows(rows, probes, colNames(g)), i)
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
        colNames(g)(0) + ": average of treated samples", false, g, false,
        TUnit.collectBarcodes(tus))
    info.addColumn(false, colNames(g)(1),
        colNames(g)(1) + ": average of control samples", false, g, false,
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

  //TODO this log2 hack (if we keep it) also applies to the FoldBuilder above.
  //However, FoldBuilder is not currently being used. We should re-evaluate whether it
  //is needed. (Actually, I'd like to unify the two different ExtFold/Fold formats and only
  //use simple Fold. - Johan

  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): Seq[ExprValue] = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head
    val fold = log2(javaMean(treatedVs, false))
    Seq(fold, new BasicExprValue(first.p, fold.call))
  }

  override protected def log2tooltips = true

  override protected def columnInfo(g: Group): ManagedMatrixInfo = {
    val tus = treatedAndControl(g)._1
    val samples = TUnit.collectBarcodes(tus)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, colNames(g)(0),
        colNames(g)(0) + ": average of treated samples", false, g, false, samples)
    info.addColumn(false, colNames(g)(1),
        colNames(g)(1) + ": p-values of treated against control", true, g, true,
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

  def makeTooltip[E <: ExprValue](data: Iterable[E], log2tfm: Boolean): String = {
    val d = if (log2tfm) data.map(log2) else data

    val r = d.filter(_.present).take(10).map(_.toString).mkString(" ")
    if (d.size > 10) {
      r + ", ..."
    } else {
      r
    }
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
    //TODO visibility of these 3 vars
    var currentInfo: ManagedMatrixInfo, var rawUngroupedMat: ExprMatrix,
    var rawGroupedMat: ExprMatrix,
    val baseColumnMap: Map[Int, Seq[Int]],
    val log2Tooltips: Boolean = false) {

  protected var currentMat: ExprMatrix = rawGroupedMat

  protected var _synthetics: Vector[Synthetic] = Vector()
  protected var _sortColumn: Option[Int] = None
  protected var _sortAscending: Boolean = false
  protected var _sortAuxTable: Option[ExprMatrix] = None

  protected var requestProbes: Seq[String] = initProbes

  currentInfo.setNumRows(currentMat.rows)

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
  def setFilterThreshold(col: Int, threshold: java.lang.Double): Unit = {
    currentInfo.setColumnFilter(col, threshold)
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

  def probesForAuxTable: Seq[String] = rawGroupedMat.orderedRowKeys

  protected def filterAndSort(): Unit = {
    def f(r: Seq[ExprValue]): Boolean = {
      for (
        col <- 0 until currentInfo.numColumns();
        thresh = currentInfo.columnFilter(col);
        if (thresh != null)
      ) {
        val av = Math.abs(r(col).value)

        //Note, comparisons with NaN are always false
        val pass = (if (currentInfo.isUpperFiltering(col))
          av <= thresh
        else
          av >= thresh)
        if (!pass || !r(col).present) {
          return false
        }
      }
      true
    }

    //TODO avoid selecting here
    currentMat = currentMat.selectNamedRows(requestProbes).filterRows(f)

    currentInfo.setNumRows(currentMat.rows)
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
        currentInfo.addColumn(true, test.getShortTitle(null), test.getTooltip(), upper, null, false,
            Array[SSample]()) //TODO
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
