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
import t.model.sample.AttributeSet
import t.testing.TestConfig
import t.{Context, TTestSuite}
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CSVRawExpressionDataTest extends TTestSuite {
  val fact = Context.factory

  test("basic") {
    val meta = TSVMetadata.apply(fact, "../OTGTool/testData/meta.tsv",
      Some(TestConfig.config.attributes), println(_))

    val d = new CSVRawExpressionData("../OTGTool/testData/data.csv", None,
      Some(meta.samples.size), m => println(s"Warning: $m"))

    d.samples should (contain theSameElementsAs(meta.samples))
    for (s <- meta.samples) {
      d.data(s).size should (be > 0)
    }
  }

  test("read typed attributes") {
    val meta = TSVMetadata.apply(fact, "../OTGTool/testData/meta.tsv", None, println(_))
    val d = new CSVRawExpressionData("../OTGTool/testData/data.csv", None,
      Some(meta.samples.size), m => println(s"Warning: $m"))
    d.samples should (contain theSameElementsAs(meta.samples))
    for (s <- meta.samples) {
      d.data(s).size should (be > 0)
    }

    val attrs = meta.attributeSet
    attrs.getAll.size should equal(71) //70 in file + batch, which can't be specified
    attrs.getAll.asScala.map(_.id) should contain allElementsOf(List("sample_id", "k", "ast", "kidney_wt_right",
      "rbc", "mcv"))
    attrs.getNumerical.asScala.size should equal(45)
  }
}
