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

package otg

import org.junit.runner.RunWith
import t.SeriesRanking.safePCorrelation
import friedrich.data.Statistics.pearsonCorrelation
import org.scalatest.junit.JUnitRunner
import t.TTestSuite

//TODO move to package t

@RunWith(classOf[JUnitRunner])
class SeriesMatchingTest extends TTestSuite {
  //	import SeriesRanking._
  //	import friedrich.data.Statistics._
  //
  //	test("Pearson correlation with missing values") {
  //	  val d1 = Series(null, null, null, 0, null, null,
  //	      data = Vector(ExprValue(1.0), ExprValue(2.0, 'A'), ExprValue(3.0)))
  //	  val d2 = Series(null, null, null, 0, null, null,
  //	      data = Vector(ExprValue(3.0), ExprValue(4.0), ExprValue(5.0)))
  //
  //	  safePCorrelation(d1, d2) should equal(pearsonCorrelation(Seq(0.0, 1.0, 3.0), Seq(0.0, 3.0, 5.0)))
  //	}
  //
  //	test("Pearson correlation with insufficient values") {
  //	  //Only one mutual present value
  //	  val d1 = Series(null, null, null, 0, null, null,
  //	      data = Vector(ExprValue(1.0), ExprValue(2.0, 'A'), ExprValue(3.0)))
  //	  val d2 = Series(null, null, null, 0, null, null,
  //	      data = Vector(ExprValue(3.0, 'A'), ExprValue(4.0), ExprValue(5.0)))
  //
  //	  val x = safePCorrelation(d1, d2)
  //	  assert(java.lang.Double.isNaN(x))
  //	}
}
