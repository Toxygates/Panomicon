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

import otgviewer.shared.ManagedMatrixInfo
import t.common.shared.probe.ProbeMapper
import t.common.shared.probe.ValueMapper
import t.common.shared.sample.FullAnnotation
import t.common.shared.sample.ExprMatrix
import t.common.shared.sample.EVArray

/**
 * A matrix mapper converts a whole matrix from one domain into
 * a matrix in another domain.
 * Example: Convert a transcript matrix into a gene matrix.
 * Example: Convert a gene matrix into a protein matrix.
 *
 * This process changes the number and index keys of the rows, but
 * preserves columns.
 */
class MatrixMapper(val pm: ProbeMapper, val vm: ValueMapper) {
  def convert(from: ExprMatrix): ExprMatrix = {
    val rangeProbes = pm.range
    val fromRowSet = from.rowKeys.toSet

    val nrows = rangeProbes.flatMap(rng => {
      val domProbes = pm.toDomain(rng).filter(fromRowSet.contains(_))

      //pull out e.g. all the rows corresponding to probes (domain)
      //for gene G1 (range)
      val domainRows = domProbes.map(dp => from.row(dp))
      if (!domainRows.isEmpty) {
        val cols = domainRows.head.size
        val nr = (0 until cols).map(c => {
              val xs = domainRows.map(dr => dr(c))
              vm.convert(rng, xs.filter(_.present))
          })

        Some((nr, FullAnnotation(rng, domProbes)))
      } else {
        None
      }
    })

    println(from.sortedColumnMap)
    val cols = (0 until from.columns).map(x => from.columnAt(x))

    val annots = nrows.map(_._2)
    ExprMatrix.withRows(nrows.map(_._1), rangeProbes, cols).copyWithAnnotations(annots)
  }

  def convert(from: ManagedMatrix): ManagedMatrix = {
    val ungr = convert(from.rawUngroupedMat)
    val gr = convert(from.rawGroupedMat)
    val rks = (0 until ungr.rows).map(ungr.rowAt)

    //Note, we re-fix initProbes for the new matrix
    new ManagedMatrix(rks, convert(from.currentInfo), ungr, gr, from.log2Tooltips)
  }

  /**
   * This conversion keeps the columns and column names (etc),
   * but removes synthetics and filtering options.
   * TODO synthetics handling needs to be tested
   */
  def convert(from: ManagedMatrixInfo): ManagedMatrixInfo = {
    val r = new ManagedMatrixInfo()
    for (i <- 0 until from.numDataColumns()) {
      r.addColumn(false, from.columnName(i), from.columnHint(i),
        from.isUpperFiltering(i), from.columnGroup(i), from.isPValueColumn(i),
        from.samples(i))
    }
    r.setPlatforms(from.getPlatforms())
    r.setNumRows(from.numRows())
    r
  }
}
