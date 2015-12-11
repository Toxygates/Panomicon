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
import t.db.ExprValue

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

  private def padToSize(vs: Iterable[ExprValue], n: Int): Iterable[ExprValue] = {
    val diff = n - vs.size
    val empty = (0 until diff).map(x => ExprValue(0, 'A'))
    vs ++ empty
  }

  /**
   * Converts grouped into (grouped, ungrouped)
   */
  private def convert(from: ExprMatrix): (ExprMatrix, ExprMatrix, Map[Int, Seq[Int]]) = {
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
              val xs = domainRows.map(dr => dr(c)).filter(_.present)
              (vm.convert(rng, xs), xs)
          })

        Some((nr, FullAnnotation(rng, domProbes)))
      } else {
        None
      }
    })

    println(from.sortedColumnMap)
    val cols = (0 until from.columns).map(x => from.columnAt(x))

    val annots = nrows.map(_._2)
    val groupedVals = nrows.map(_._1.map(_._1))

    //the max. size of each ungrouped column
    val ungroupedSizes = (0 until from.columns).map(c => nrows.map(_._1(c)._2.size).max)
    //pad to size to get a matrix
    val ungroupedVals = for (r <- nrows)
      yield (0 until from.columns).flatMap(c => padToSize(r._1(c)._2, ungroupedSizes(c)))

    //The base map will be used for generating tooltips from the ungrouped matrix
    //that we constructed above
    var at = 0
    var baseMap = Map[Int, Seq[Int]]()
    for (i <- 0 until from.columns) {
      baseMap += i -> (at until (at + ungroupedSizes(i)))
      at += ungroupedSizes(i)
    }

    val ungroupedColNames = (0 until ungroupedVals(0).size).map(i => s"Ungrouped-$i")
    val grouped = ExprMatrix.withRows(groupedVals, rangeProbes, cols).copyWithAnnotations(annots)
    val ungrouped = ExprMatrix.withRows(ungroupedVals, rangeProbes, ungroupedColNames)
    (grouped, ungrouped, baseMap)
  }

  def convert(from: ManagedMatrix): ManagedMatrix = {
//    val ungr = convert(from.rawUngroupedMat)
    val (gr, ungr, bm) = convert(from.rawGroupedMat)
    val rks = (0 until ungr.rows).map(ungr.rowAt)

    //Note, we re-fix initProbes for the new matrix
    new ManagedMatrix(rks, convert(from.currentInfo), ungr, gr, bm, false)
  }

  /**
   * This conversion keeps the columns and column names (etc),
   * but removes synthetics and filtering options.
   * TODO synthetics handling needs to be tested
   */
  private def convert(from: ManagedMatrixInfo): ManagedMatrixInfo = {
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
