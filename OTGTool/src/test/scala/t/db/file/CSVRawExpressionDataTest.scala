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

package t.db.file

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.TTestSuite
import t.db.testing.TestData
import otg.OTGContext
import t.testing.TestConfig

@RunWith(classOf[JUnitRunner])
class CSVRawExpressionDataTest extends TTestSuite {
  import TestData._
  val fact = OTGContext.factory
  val meta = TSVMetadata.apply(fact, "testData/meta.tsv",
    TestConfig.config.attributes, println(_))

  test("basic") {
    val d = new CSVRawExpressionData("testData/data.csv", None,
      Some(meta.samples.size), m => println(s"Warning: $m"))

    d.samples should (contain theSameElementsAs(meta.samples))
    for (s <- meta.samples) {
      d.data(s).size should (be > 0)
    }
  }
}
