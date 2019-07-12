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
import scala.reflect.ClassTag

/**
 * Routines for loading a ManagedMatrix and constructing groups.
 */
abstract class ManagedMatrixBuilder[E <: ExprValue : ClassTag](reader: MatrixDBReader[E], val probes: Seq[String]) {
  import ManagedMatrix._

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
  protected def columnsFor(g: Group, sortedSamples: Seq[Sample],
    data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo)

  /**
   * Collapse multiple raw expression values into a single cell.
   */
  protected def buildValue(raw: RowData): ExprValue

  /**
   * Default tooltip for columns
   */
  protected def tooltipSuffix: String = ": average of treated samples"

  protected def shortName(g: Group): String = g.toString

  protected def defaultColumns[E <: ExprValue](g: Group, sortedSamples: Seq[Sample],
    data: Seq[RowData]): (Seq[RowData], ManagedMatrixInfo) = {
    // A simple average column
    val tus = treatedAndControl(g)._1
    val treatedIdx = unitIdxs(tus, sortedSamples)
    val samples = TUnit.collectSamples(tus)

    val info = new ManagedMatrixInfo()

    info.addColumn(false, shortName(g), g.toString,
        s"$g$tooltipSuffix",
        ColumnFilter.emptyAbsGT, g, false, samples)
    val d = data.map(vs => Seq(buildValue(selectIdx(vs, treatedIdx))))

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
        packedProbes, sparseRead, false).map(_.toSeq).toSeq

    val sortedProbes = data.map(row => row(0).probe)
    val annotations = sortedProbes.map(x => new SimpleAnnotation(x)).toVector

    val cols = requestColumns.par.map(g => {
        println(g.getUnits()(0).toString())
        columnsFor(g, sortedSamples, data)
    }).seq

    val (groupedData, info) = cols.par.reduceLeft((p1, p2) => {
      val d = (p1._1 zip p2._1).map(r => r._1 ++ r._2)
      val info = p1._2.addAllNonSynthetic(p2._2)
      (d, info)
    })
    val colNames = (0 until info.numColumns()).map(i => info.columnName(i))
    val grouped = ExprMatrix.withRows(groupedData, sortedProbes, colNames)

    var ungrouped = ExprMatrix.withRows(data.toSeq.map(_.toSeq),
        sortedProbes, sortedSamples.map(_.sampleId))

    val baseColumns = Map() ++ (0 until info.numDataColumns()).map(i => {
      val sampleIds = info.samples(i).map(_.id).toSeq
      val sampleIdxs = sampleIds.map(i => ungrouped.columnMap.get(i)).flatten
      (i -> sampleIdxs)
    })

    ungrouped = finaliseUngrouped(ungrouped)

    new ManagedMatrix(
      LoadParams(sortedProbes, info,
        ungrouped.copyWithAnnotations(annotations),
        grouped.copyWithAnnotations(annotations),
        baseColumns)
      )
  }

  protected def finaliseUngrouped(ungr: ExprMatrix): ExprMatrix = ungr

  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))
  final protected def javaMean[E <: ExprValue](data: Iterable[E], presentOnly: Boolean = true) = {
    val m = ExprValue.mean(data, presentOnly)
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

  protected def treatedAndControl(g: Group) = {
    val sc = g.getSchema
    g.getUnits().partition(u => !sc.isControl(u))
  }
}

trait TreatedControlBuilder[E <: ExprValue] {
  this: ManagedMatrixBuilder[E] =>

  type RowData = ManagedMatrix.RowData

  def enhancedColumns: Boolean

  protected def buildRow(raw: Seq[E],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData

  protected def columnInfo(g: Group): ManagedMatrixInfo
  def colNames(g: Group): Seq[String]

  protected def columnsFor(g: Group, sortedSamples: Seq[Sample],
    data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo) = {

    val (treatedUnits, controlUnits) = treatedAndControl(g)
    println(s"#Control units: ${controlUnits.size} #Non-control units: ${treatedUnits.size}")

    if (treatedUnits.size > 1 || (!enhancedColumns) || controlUnits.size == 0 || treatedUnits.size == 0) {
      // A simple average column
      defaultColumns(g, sortedSamples, data)
    } else if (treatedUnits.size == 1) {
      // Possibly insert a control column as well as the usual one

      val ti = unitIdxs(treatedUnits, sortedSamples)
      val ci = unitIdxs(controlUnits, sortedSamples)

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
class NormalizedBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes)
    with TreatedControlBuilder[PExprValue] {

  protected def buildValue(raw: RowData): ExprValue = ExprValue.presentMean(raw)

  override protected def shortName(g: Group): String = "Treated"

  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData =
    Seq(buildValue(selectIdx(raw, treatedIdx)),
      buildValue(selectIdx(raw, controlIdx)))

  protected def columnInfo(g: Group) = {
    val (tus, cus) = treatedAndControl(g)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, shortName(g), colNames(g)(0),
        colNames(g)(0) + ": average of treated samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectSamples(tus))
    info.addColumn(false, "Control", colNames(g)(1),
        colNames(g)(1) + ": average of control samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectSamples(cus))
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

  protected def buildValue(raw: RowData): ExprValue = log2(javaMean(raw))

  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head
    val fold = buildValue(treatedVs)
    Seq(fold, new BasicExprValue(first.p, fold.call))
  }

  override protected def finaliseUngrouped(ungr: ExprMatrix) =
    ungr.map(e => ManagedMatrix.log2(e))

  override protected def shortName(g: Group) = "Log2-fold"

  override protected def tooltipSuffix = ": log2-fold change of treated versus control"

  override protected def columnInfo(g: Group): ManagedMatrixInfo = {
    val tus = treatedAndControl(g)._1
    val samples = TUnit.collectSamples(tus)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, shortName(g), colNames(g)(0),
        colNames(g)(0) + tooltipSuffix,
        ColumnFilter.emptyAbsGT, g, false, samples)
    info.addColumn(false, "P-value", colNames(g)(1),
        colNames(g)(1) + ": p-values of treated against control",
        ColumnFilter.emptyLT, g, true,
        Array[SSample]())
    info
  }

  def colNames(g: Group) =
    List(g.toString, g.toString + "(p)")
}
