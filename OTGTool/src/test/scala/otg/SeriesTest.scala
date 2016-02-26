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
import t.db.SeriesDB
import t.db.kyotocabinet.KCSeriesDB
import org.scalatest.junit.JUnitRunner
import otg.Species._
import t.testing.TestConfig
import t.db.testing.TestData
import t.TTestSuite
import t.db.BasicExprValue
import t.db.SeriesPoint

@RunWith(classOf[JUnitRunner])
class SeriesTest extends TTestSuite {

  import otg.testing.{TestData => OData}
  implicit val context = new otg.testing.FakeContext()
  val cmap = context.enumMaps("compound_name")

  test("normalization") {

  }
}
