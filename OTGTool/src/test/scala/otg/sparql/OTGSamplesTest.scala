/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otg.sparql

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import otg.OTGMatrixContext
import t.platform.Species.Rat
import t.TTestSuite
import t.model.shared.SampleClassHelper
import t.sparql._
import t.testing.TestConfig
import otg.OTGSeries
import otg.model.sample.OTGAttribute._

@RunWith(classOf[JUnitRunner])
class OTGSamplesTest extends TTestSuite {

  val config = TestConfig.config
  implicit val context = new OTGMatrixContext(config)
  val samples = new OTGSamples(config)

  after {
    samples.close
  }

  val baseConstraints = Map(
    Organism -> "Rat",
    TestType -> "in vivo")

  val fullConstraints = SampleClassHelper(Map(
      Organ -> "Liver",
      Repeat -> "Single") ++ baseConstraints)

  implicit val sampleFilter = SampleFilter()

  test("organs") {
    val sf = SampleClassFilter(
        SampleClassHelper(Map(
      Organ -> "Kidney",
      Repeat -> "Repeat") ++ baseConstraints)
      ).filterAll

    val os = samples.sampleAttributeQuery(Organ).
      constrain(sf)()

    os.toSet should (contain("Kidney"))
  }

  test("dose levels") {
    val sf = SampleClassFilter(fullConstraints).filterAll

    val ds = samples.sampleAttributeQuery(DoseLevel).
      constrain(sf)()

    //TODO use the unified DataSchema instead
    assert(ds.toSet === Set("Low", "Middle", "High", "Control"))
  }

  test("times") {
    val sf = SampleClassFilter(fullConstraints).filterAll

    val ts = samples.sampleAttributeQuery(ExposureTime).
      constrain(sf)()

    //TODO use the unified DataSchema instead
    assert(OTGSeries.singleVivoExpected.toSet subsetOf ts.toSet)
  }
}
