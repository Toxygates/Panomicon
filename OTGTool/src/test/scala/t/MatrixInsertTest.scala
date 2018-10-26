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

import org.junit.runner.RunWith

import t.db._
import t.db.testing.FakeBasicMatrixDB
import t.db.testing.TestData
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.scalatest.junit.JUnitRunner
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class MatrixInsertTest extends TTestSuite {

  import t.db.testing.TestData._

  def await[T](task: Task[T]) = {
    val fut = TaskRunner.runThenFinally(task)(())
    Await.ready(fut, Duration.Inf)
  }

  test("Absolute value insertion") {
    val data = makeTestData(false, (samples take 10))
    val db = new FakeBasicMatrixDB()

    val ins = new BasicValueInsert(db, data) {
      def mkValue(v: FoldPExpr) =
       BasicExprValue(v._1, v._2)
    }
    await(ins.insert("Absolute value data insert"))

    val ss1 = db.records.map(_._1).distinct
    val ss2 = data.samples
    ss1 should (contain theSameElementsAs ss2)

    for (sample <- ss1) {
      val vs1 = db.records.filter(_._1 == sample).map(ev =>
        (context.probeMap.unpack(ev._2), ev._3.value, ev._3.call))
      val vs2 = for {
        (probe, PExprValue(expr, p, call, prb)) <- data.asExtValues(sample)
      } yield (probe, expr, call)

      vs1 should (contain theSameElementsAs vs2)
    }

    db.released should be(true)
  }

  //  test("Folds") {
  //    val data = makeTestData()
  //    val db = new FakeMicroarrayDB()
  //    val builder = new BasicFoldValueBuilder()
  //  }
}
