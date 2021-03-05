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

package t.viewer.server.rpc

import scala.collection.JavaConversions._

import org.apache.commons.lang.StringUtils

import t.common.shared.ValueType
import t.platform.Probe
import t.sparql.ProbeStore
import t.viewer.server.matrix.MatrixController
import t.viewer.server.matrix.ManagedMatrix


class ClusteringData(val controller: MatrixController,
                     probeStore: ProbeStore,
                     rows: Seq[String],
                     valueType: ValueType) extends t.clustering.server.ClusteringData {

  val mm = controller.managedMatrix
  val mat = if (rows != null && rows.length > 0) {
    mm.current.selectRowsFromAtomics(rows)
  } else {
    mm.current
  }

  val info = mm.info

  val allRows = mat.asRows

  val columns = mat.columnKeys.zipWithIndex.filter(x => !info.isPValueColumn(x._2))

  private def joinedAbbreviated(items: Iterable[String], n: Int): String =
    StringUtils.abbreviate(items.toSeq.distinct.mkString("/"), 30)

  def rowNames: Array[String] =
    allRows.map(r => joinedAbbreviated(r.atomicProbes, 20)).toArray

  def colNames: Array[String] = columns.map(_._1).toArray

  def codeDir: String = ???

  /**
   * Obtain row-major data for the specified rows and columns
   */
  def data: Array[Array[Double]] = mat.selectColumns(columns.map(_._2)).rowData.
    map(_.map(_.value).toArray).toArray

  def ungroupedNames: Array[String] = {
    val baseColumns = columns.flatMap(c => mm.baseColumns(c._2))
    baseColumns.map(c => mm.rawUngrouped.columnKeys(c))
  }

  def ungroupedSamples = {
    val baseColumns = columns.flatMap(c => mm.baseColumns(c._2))
    baseColumns.map(mm.rawUngrouped.columnKeys)
  }

  /**
   * Obtain column-major data for individual (ungrouped) samples underlying the columns
   * Ignores "rows" parameter
   */
  def ungroupedData: Array[Array[Double]] = {
    val baseColumns = columns.flatMap(c => mm.baseColumns(c._2))
    val useMat = mm.rawUngrouped.selectColumns(baseColumns)
    val useRows = if (rows == null || rows.length == 0) {
      useMat.rowData
    } else {
      useMat.selectNamedRows(rows).rowData
    }
    useRows.map(_.map(_.value).toArray).toArray
  }

  /**
   * Gene symbols for the specified rows
   */
  def geneSymbols: Array[String] = {
    val allAtomics = allRows.flatMap(_.atomicProbes.map(p => Probe(p)))
    val aaLookup = Map() ++ probeStore.withAttributes(allAtomics).map(a => a.identifier -> a)

    allRows.map(r => {
      val atrs = r.atomicProbes.map(aaLookup(_))
      joinedAbbreviated(atrs.flatMap(_.symbols), 20)
    }).toArray
  }
}
