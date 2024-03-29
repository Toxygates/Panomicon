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

package otg

import t.SeriesRanking.safePCorrelation
import friedrich.data.Statistics.pearsonCorrelation
import org.scalatest.junit.JUnitRunner
import t.{OTGSeries, TTestSuite, TimeSeries}
import t.db._
import t.testing.{FakeContext, TestConfig}
import t.db.testing.DBTestData
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SeriesMatchingTest extends TTestSuite {
  import friedrich.data.Statistics._

  val config = TestConfig.config
  implicit val context = new FakeContext()

  def mkSeries(points: Seq[ExprValue]) = {
    val doses = context.enumMaps("dose_level")
    val dps = Seq("Low", "Middle", "High").map(doses)
    dps.zip(points).map(p => SeriesPoint(p._1, p._2))
  }

  test("Pearson correlation with missing values") {
    val d1 = OTGSeries(TimeSeries, null, null, null, 0, null, null, null,
      mkSeries(Seq(ExprValue(1.0), ExprValue(2.0, 'A'), ExprValue(3.0))))
    val d2 = OTGSeries(TimeSeries, null, null, null, 0, null, null, null,
      mkSeries(Seq(ExprValue(3.0), ExprValue(4.0), ExprValue(5.0))))

    safePCorrelation(d1, d2) should equal(pearsonCorrelation(List(0.0, 1.0, 3.0), List(0.0, 3.0, 5.0)))
  }

  test("Pearson correlation with insufficient values") {
    //Only one mutual present value
    val d1 = OTGSeries(TimeSeries, null, null, null, 0, null, null, null,
      mkSeries(Seq(ExprValue(1.0), ExprValue(2.0, 'A'), ExprValue(3.0))))
    val d2 = OTGSeries(TimeSeries, null, null, null, 0, null, null, null,
      mkSeries(Seq(ExprValue(3.0, 'A'), ExprValue(4.0), ExprValue(5.0))))

    val x = safePCorrelation(d1, d2)
    assert(java.lang.Double.isNaN(x))
  }
}
