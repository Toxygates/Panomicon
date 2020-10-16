/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import t.common.shared.sample.Group
import t.common.shared.sample.{Sample => SSample}
import t.common.shared.sample.{Unit => TUnit}
import t.db._
import t.viewer.shared.ColumnFilter
import t.viewer.shared.ManagedMatrixInfo

import scala.reflect.ClassTag

/**
 * Routines for loading a ManagedMatrix and constructing groups.
 */
abstract class ManagedMatrixBuilder[E <: ExprValue : ClassTag](reader: MatrixDBReader[E], val probes: Seq[String]) {
  import ManagedMatrix._

  def build(requestColumns: Seq[Group], sparseRead: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    loadRawData(requestColumns, reader, sparseRead)
  }

  /**
   * Construct the columns representing a particular group (g), from the given
   * raw data, and column info reflecting these columns.
   */
  def columnsFor(g: Group, sortedSamples: Seq[Sample],
                          data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo) = {
    val (treatedUnits, controlUnits) = treatedAndControl(g)
    println(s"#Control units: ${controlUnits.size} #Non-control units: ${treatedUnits.size}")
    val ti = unitIdxs(treatedUnits, sortedSamples)
    val ci = unitIdxs(controlUnits, sortedSamples)
    columnsFor(g, treatedUnits, ti, controlUnits, ci, data)
  }

  def columnsFor(g: Group, treatedUnits: Array[TUnit], treatedIdx: Seq[Int],
                 controlUnits: Array[TUnit], controlIdx: Seq[Int],
                 data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo)

  /**
   * Collapse multiple raw expression values into a single cell.
   */
  protected def buildValue(raw: Seq[ExprValue]): BasicExprValue

  /**
   * Flatten an ExprValue into a BasicExprValue, potentially losing information.
   */
  protected def asBasicValue(v: ExprValue): BasicExprValue = {
    v match {
      case b: BasicExprValue => b
      case _ => BasicExprValue(v.value, v.call, v.probe)
    }
  }

  /**
   * Default tooltip for columns
   */
  protected def tooltipSuffix: String = ": average of treated samples"

  protected def shortName(g: Group): String = g.toString

  protected def defaultColumns[E <: ExprValue](g: Group, treatedIdx: Seq[Int],
                                               treatedUnits: Array[TUnit],
    data: Seq[Seq[E]]): (Seq[RowData], ManagedMatrixInfo) = {
    val samples = TUnit.collectSamples(treatedUnits)

    val info = new ManagedMatrixInfo()

    info.addColumn(false, shortName(g), g.toString,
        s"$g$tooltipSuffix",
        ColumnFilter.emptyAbsGT, g, false, samples)
    val d = data.map(vs => Seq(buildValue(selectIdx(vs, treatedIdx))))

    (d, info)
  }

  def loadRawData(requestColumns: Seq[Group],
    reader: MatrixDBReader[E], sparseRead: Boolean)(implicit context: MatrixContext): ManagedMatrix = {
    val packedProbes = probes.map(context.probeMap.pack)

    val samples = requestColumns.flatMap(g => g.getSamples).distinct

    val sortedSamples = reader.sortSamples(samples.map(b => Sample(b.id)))
    val data = reader.valuesForSamplesAndProbes(sortedSamples,
        packedProbes, sparseRead, false).map(_.toSeq).
        filter(row => row.exists(_.isPadding == false))

    val sortedProbes = data.map(row => row(0).probe)
    val annotations = sortedProbes.map(x => RowAnnotation(x, List(x))).toVector

    val columnsForGroups = requestColumns.map(g => {
        println(g.getUnits()(0).toString())
        columnsFor(g, sortedSamples, data)
    })

    val (groupedData, info) = columnsForGroups.reduceLeft((g1Cols, g2Cols) => {
      val rowData = (g1Cols._1 zip g2Cols._1).map(r => r._1 ++ r._2)
      val jointInfo = g1Cols._2.addAllNonSynthetic(g2Cols._2)
      (rowData, jointInfo)
    })
    val colNames = (0 until info.numColumns()).map(i => info.columnName(i))
    val grouped = ExpressionMatrix.withRows(groupedData, sortedProbes, colNames)

    var ungrouped = ExpressionMatrix.withRows(data.toSeq.map(_.map(asBasicValue).toSeq),
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

  protected def finaliseUngrouped(ungr: ExpressionMatrix): ExpressionMatrix = ungr

  final protected def selectIdx[E <: ExprValue](data: Seq[E], is: Seq[Int]) = is.map(data(_))

  protected def unitIdxs(us: Iterable[t.common.shared.sample.Unit], samples: Seq[Sample]): Seq[Int] = {
    val ids = us.flatMap(u => u.getSamples.map(_.id)).toSet
    val inSet = samples.map(s => ids.contains(s.sampleId))
    inSet.zipWithIndex.filter(_._1).map(_._2)
  }

  protected def treatedAndControl(g: Group) = {
    val sc = g.getSchema
    g.getUnits().partition(u => !sc.isControl(u))
  }
}
/**
 * Columns consisting of normalized intensity / "absolute value" expression data
 * for both treated and control samples.
 */
class NormalizedBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes) {
  import ManagedMatrix._

  protected def buildValue(raw: Seq[ExprValue]): BasicExprValue = ExprValue.presentMean(raw)

  override protected def shortName(g: Group): String = treatedColumnShortName

  protected def buildRow(treated: Seq[PExprValue],
                         control: Seq[PExprValue]): RowData =
    Seq(buildValue(treated), buildValue(control))

  protected def columnInfo(g: Group) = {
    val (tus, cus) = treatedAndControl(g)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, shortName(g), colNames(g)(0),
        colNames(g)(0) + ": average of treated samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectSamples(tus))
    info.addColumn(false, controlColumnShortName, colNames(g)(1),
        colNames(g)(1) + ": average of control samples", ColumnFilter.emptyAbsGT, g, false,
        TUnit.collectSamples(cus))
    info
  }

  def colNames(g: Group): Seq[String] =
    List(g.toString, g.toString + "(cont)")

  def columnsFor(g: Group, treatedUnits: Array[TUnit], treatedIdx: Seq[Int],
                 controlUnits: Array[TUnit], controlIdx: Seq[Int],
                 data: Seq[Seq[PExprValue]]) = {
    if (!enhancedColumns) {
      // A simple average column
      defaultColumns(g, treatedIdx, treatedUnits, data)
    } else {
      val rows = data.map(vs => buildRow(selectIdx(vs, treatedIdx),
        selectIdx(vs, controlIdx)))
      val i = columnInfo(g)
      (rows, i)
    }
  }
}

/**
 * Columns consisting of fold-values, associated p-values and custom P/A calls.
 */
class ExtFoldBuilder(val enhancedColumns: Boolean, reader: MatrixDBReader[PExprValue],
  probes: Seq[String]) extends ManagedMatrixBuilder[PExprValue](reader, probes) {
  import ManagedMatrix._

  protected def buildValue(raw: Seq[ExprValue]): BasicExprValue =
    ExprValue.log2(ExprValue.mean(raw, true))

  protected def buildRow(raw: Seq[PExprValue],
    treatedIdx: Seq[Int], controlIdx: Seq[Int]): RowData = {
    val treatedVs = selectIdx(raw, treatedIdx)
    val first = treatedVs.head
    val fold = buildValue(treatedVs)
    Seq(fold, new BasicExprValue(first.p, fold.call))
  }

  override protected def finaliseUngrouped(ungr: ExpressionMatrix) =
    ungr.map(e => ExprValue.log2(e))

  override protected def shortName(g: Group) = log2FoldColumnShortName

  override protected def tooltipSuffix = ": log2-fold change of treated versus control"

  protected def columnInfo(g: Group): ManagedMatrixInfo = {
    val tus = treatedAndControl(g)._1
    val samples = TUnit.collectSamples(tus)
    val info = new ManagedMatrixInfo()
    info.addColumn(false, shortName(g), colNames(g)(0),
        colNames(g)(0) + tooltipSuffix,
        ColumnFilter.emptyAbsGT, g, false, samples)
    info.addColumn(false, pValueColumnShortName, colNames(g)(1),
        colNames(g)(1) + ": p-values of treated against control",
        ColumnFilter.emptyLT, g, true,
        Array[SSample]())
    info
  }

  def colNames(g: Group) =
    List(g.toString, g.toString + "(p)")

  def columnsFor(g: Group, treatedUnits: Array[TUnit], treatedIdx: Seq[Int],
                 controlUnits: Array[TUnit], controlIdx: Seq[Int],
                 data: Seq[Seq[PExprValue]]) = {
    if (treatedUnits.size != 1 || !enhancedColumns) {
      // A simple average column
      defaultColumns(g, treatedIdx, treatedUnits, data)
    } else {
      val rows = data.map(vs => buildRow(vs, treatedIdx, controlIdx))
      val i = columnInfo(g)
      (rows, i)
    }
  }
}
