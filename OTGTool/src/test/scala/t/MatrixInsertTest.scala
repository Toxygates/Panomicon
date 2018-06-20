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

package t

import t.db._
import t.db.testing.FakeBasicMatrixDB
import t.testing.FakeContext
import t.db.testing.TestData
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MatrixInsertTest extends TTestSuite {

  import TestData._

  test("Absolute value insertion") {
    val data = makeTestData(false, (samples take 10))
    val db = new FakeBasicMatrixDB()

    val ins = new BasicValueInsert(db, data) {
      def mkValue(v: FoldPExpr) =
       BasicExprValue(v._1, v._2)
    }

    ins.insert("Absolute value data insert").execute()

    val s1 = db.records.map(ev => (ev._1, context.probeMap.unpack(ev._2),
        ev._3.value, ev._3.call))
    val s2 = for {
      s <- data.samples;
      (probe, PExprValue(expr, p, call, prb)) <- data.asExtValues(s)
    } yield (s, probe, expr, call)
          
    s1 should (contain theSameElementsAs s2)

    db.released should be(true)
  }

  //  test("Folds") {
  //    val data = makeTestData()
  //    val db = new FakeMicroarrayDB()
  //    val builder = new BasicFoldValueBuilder()
  //  }
}
