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

package otg.sparql

import org.junit.runner.RunWith
import otg.OTGContext
import otg.Species.Rat
import t.testing.TestConfig
import org.scalatest.junit.JUnitRunner
import t.sparql._
import otg.OTGTestSuite

@RunWith(classOf[JUnitRunner])
class OTGSamplesTest extends OTGTestSuite {

  val config = TestConfig.config
  implicit val context = new OTGContext(config)
  val samples = new OTGSamples(config)

  after {
    samples.close
  }

  //TODO update tests

  //  test("organs") {
  //    val f = Filter(Some(Kidney), Some(Repeat), Some(Rat), Some("in vivo"))
  //    val os = samples.organs(f, None)
  //    //NA organs are for those samples that have no arrays
  //    assert(os.toSet === Set("Liver", "Kidney"))
  //  }
  //
  //  test("dose levels") {
  //    val f = Filter(Some(Liver), Some(Single), Some(Rat), Some("in vivo"))
  //    val ds = samples.doseLevels(f, None)
  //    //TODO use the unified DataSchema instead
  ////    assert(ds.toSet === TimeDose.doses.toSet)
  //  }
  //
  //  test("times") {
  //    val f = Filter(Some(Liver), Some(Single), Some(Rat), Some("in vivo"))
  //    val ts = samples.times(f, None)
  //   //TODO use the unified DataSchema instead
  ////    assert(ts.toSet.subsetOf(TimeDose.times.toSet))
  //  }
  //
  //  test("times and doses") {
  //    val f = Filter(Some(Liver), Some(Single), Some(Rat), Some("in vivo"))
  //    val compound = "acetaminophen"
  //    val td = samples.timeDoseCombinations(f)
  //    assert(td.size === 16)
  //    val s = td.toSet
  //    assert(s.contains(("24 hr", "Control")))
  //    assert(s.contains(("3 hr", "Middle")))
  //    assert(s.contains(("9 hr", "High")))
  //  }

  //  test("barcodes") {
  //    val bcs = samples.barcodes(Filter(Some(Liver), None, None), Some("adapin"), None, None)
  //    bcs.size should equal(84)
  //    println(bcs)
  //  }

}
