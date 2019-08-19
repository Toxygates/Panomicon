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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import otg.viewer.server.rpc.Conversions._
import t.TTestSuite
import t.common.shared.sample._
import t.db.testing._
import t.db.testing.TestData._
import org.scalactic.source.Position.apply

@RunWith(classOf[JUnitRunner])
class ManagedMatrixTest extends TTestSuite {
  import TestData._
  import t.common.testing.TestData.groups

  val schema = t.common.testing.TestData.dataSchema

  //Note: the FakeContext used for testing doesn't define an absoluteDBReader currently
  def normBuilder = new NormalizedBuilder(false, context.absoluteDBReader,
      probes.map(probeMap.unpack))

  def foldBuilder = new ExtFoldBuilder(false, context.foldsDBReader,
      probes.map(probeMap.unpack))

  context.populate()

  test("build") {
    val m = foldBuilder.build(groups, false, true)
    val cur = m.current
    val sortedProbes = probes.sorted.map(probeMap.unpack)

    cur.rows should equal(probes.size)
    cur.columns should equal(groups.size)
    cur.sortedRowMap.map(_._1) should equal(sortedProbes)

    val info = m.info
    val colNames = (0 until info.numColumns()).map(info.columnName)
    colNames should equal(groups.map(_.getName))

    val raw = m.rawUngrouped
    raw.columns should equal(10)
    raw.rows should equal(probes.size)
    raw.sortedRowMap.map(_._1) should equal(sortedProbes)

    val gr = m.rawGrouped
    gr.rows should equal(probes.size)
    gr.sortedRowMap.map(_._1) should equal(sortedProbes)
  }

  test("sort and select") {
    val m = foldBuilder.build(groups, false, true)
    val ps = TestData.probes.take(10).map(probeMap.unpack)

    val preSort = m.current

    m.sort(0, false)
    m.selectProbes(ps)

    var mat = m.current
    mat.rows should equal(ps.size)
    val srm = mat.sortedRowMap

    mat = m.rawGrouped
    mat.rows should equal(probes.size)

    mat = m.rawUngrouped
    mat.rows should equal(probes.size)

    for (p <- ps) {
      preSort.row(p) should equal(m.current.row(p))
      preSort.row(p) should equal(m.rawGrouped.row(p))
    }
  }

//
//  test("adjoined sorting") {
//    val em = testMatrix
//    val adjData = List(List(3),
//         List(1),
//         List(2),
//         List(5),
//         List(4)).map(xs => EVArray(xs.map(new ExpressionValue(_))))
//    val em2 = ExprMatrix.withRows(adjData)
//
//  }
}
