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

package t.server.viewer.matrix

import t.server.viewer.CSVHelper
import t.sparql.ProbeStore
import t.platform.Probe

object CSVDownload {

  /**
   * Generate a downloadable CSV file.
   * @param managedMat matrix data
   * @param probeStore probe data source
   * @param directory the directory to place the file in
   * @param individualSamples should columns be samples or groups?
   * @param auxColumns function to generate auxiliary columns, if any.
   * @return the name of the file generated in the directory.
   */
  def generate(managedMat: ManagedMatrix, probeStore: ProbeStore,
               directory: String, individualSamples: Boolean): String = {

     var mat = if (individualSamples &&
      managedMat.rawUngrouped != null && managedMat.current != null) {
      //Individual samples
      val info = managedMat.info
      val keys = managedMat.current.rowKeys.toSeq
      val ungrouped = managedMat.rawUngrouped.selectNamedRows(keys)
      val parts = (0 until info.numDataColumns).map(g => {
        if (!info.isPValueColumn(g)) {
          //Help the user by renaming the columns.
          //Prefix sample IDs by group IDs.

          val ids = info.samples(g).map(_.id)
          val ungroupedSel = ungrouped.selectNamedColumns(ids)
          val newColNames = ungroupedSel.columnKeys.map(x => info.columnName(g) + ":" + x)
          ungroupedSel.copyWith(ungroupedSel.rowData, ungroupedSel.rowKeys, newColNames)
        } else {
          //p-value column, present as it is
          managedMat.current.selectColumns(List(g))
        }
      })

      parts.reduce(_ adjoinRight _)
    } else {
      //Grouped, no editing needed
      managedMat.current
    }

    val colNames = mat.columnKeys
    val rows = mat.asRows
    //Task: move into RowLabels if possible
    val rowNames = rows.map(_.atomicProbes.mkString("/"))

    //May be slow!
    val gis = probeStore.allGeneIds.mapInnerValues(_.identifier)
    val atomics = rows.map(_.atomicProbes)
    val geneIds = atomics.map(row =>
      row.flatMap(at => gis.getOrElse(Probe(at), Seq.empty))).map(_.distinct.mkString(" "))

    val aux = List(("Gene", geneIds))
    CSVHelper.writeCSV("toxygates", directory,
      aux, rowNames, colNames,
      mat.rowData.map(_.map(_.value)))
  }

}
