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

package t.platform

import t.model.shared.SampleClassHelper._
import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._
import t.model.sample.OTGAttribute._
import t.model.sample.CoreParameter._
import t.model.sample.Attribute
import t.testing.TestData

@RunWith(classOf[JUnitRunner])
class ControlGroupTest extends TTestSuite {
  val metadata = TestData.metadata
  import t.db.testing.DBTestData.enumValues
  val bioParams = TestData.bioParameters
  val samples = t.db.testing.DBTestData.samples

  test("basic") {
    for (s <- samples) {
      val cg = TestData.controlGroups(s)
      cg.samples.size should equal(3)

      val remove = Set[Attribute](DoseLevel, Individual, LiverWeight, KidneyWeight, Treatment)

      //Except for the removed keys, the sample classes of each sample in the control
      //group should be equal to the sample class of s.
      for (cs <- cg.samples) {
        cs.sampleClass.asScalaMap.filter(c => ! remove.contains(c._1)) should
          equal(s.sampleClass.asScalaMap.filter(c => ! remove.contains(c._1)))
      }
    }
  }

  test("liver weight") {
    for (s <- samples;
      cg = TestData.controlGroups(s)) {
      val isControl = cg.samples.toSet.contains(s)

      if (isControl || s.sampleClass(Individual) == "2") {
         //healthy
        metadata.sampleAttribute(s, LiverWeight).get.toDouble should
          be (3.0 +- 0.1)
      } else {
        //unhealthy
        metadata.sampleAttribute(s, LiverWeight).get.toDouble should
          be (5.0 +- 0.1)
      }

      val time = metadata.sampleAttribute(s, ExposureTime).get
      println(cg.paramVals)

      /*
       * These limits may need to be adjusted in the future.
       */
      cg.lowerBound(LiverWeight, 1).doubleValue should
        be (2.9 +- 0.2)
      cg.upperBound(LiverWeight, 1).doubleValue should
        be (3.1 +- 0.2)
    }
  }
}
