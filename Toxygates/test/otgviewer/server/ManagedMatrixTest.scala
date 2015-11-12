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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.common.shared.sample._
import t.db.testing._
import t.db.testing.TestData._
import org.scalatest.FunSuite
import otgviewer.server.rpc.Conversions._

@RunWith(classOf[JUnitRunner])
class ManagedMatrixTest extends FunSuite {

  implicit val testContext = TestData
  val schema = t.common.testing.TestData.dataSchema()

  def normBuilder = new NormalizedBuilder(false, testContext.matrix,
      TestData.probes)

  val groups = TestData.samples.take(10).grouped(2).zipWithIndex.map(ss => {
    val sss = ss._1.map(s => asJavaSample(s))
    new Group(schema, "Gr" + ss._2, sss.toArray)
  }).toSeq

  test("build") {
    val m = normBuilder.build(groups, false, true)
    val cur = m.current
    assert (cur.rows === TestData.probes.size)
    assert (cur.columns === groups.size)

    val raw = m.rawUngroupedMat
    assert (raw.columns === 10)
    assert (raw.rows === cur.rows)

    val info = m.info
    val colNames = (0 until info.numColumns()).map(info.columnName)
    assert (colNames === (0 until 5).map(g => s"Gr$g"))

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
