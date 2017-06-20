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

package otg

import t.db.SeriesDB
import t.db.kyotocabinet.KCSeriesDB
import org.scalatest.junit.JUnitRunner
import otg.Species._
import t.testing.TestConfig
import t.db.testing.TestData
import t.TTestSuite
import t.db.BasicExprValue
import t.db.SeriesPoint
import t.db.kyotocabinet.KCExtMatrixDB
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SeriesTest extends TTestSuite {

  import otg.testing.{TestData => OData}
  implicit val context = new otg.testing.FakeContext()
  val cmap = context.enumMaps("compound_name")

  TestData.makeTestData(false)

  test("pack and build") {
    for (s <- OData.series) {
      val p = OTGSeries.pack(s)
      val b = OTGSeries.build(p, s.probe)
      b should equal(s.copy(points = Seq()))
    }
  }

//  test("normalization") {
//  }

  test("makeNew") {
    context.populate()
    val meta = OData.metadata
    val timeMap = context.enumMaps("exposure_time")

    val ss = OTGSeries.makeNew(context.foldsDBReader, meta)
    val data = context.testData
    for (s <- ss;
        const = s.constraints.filter(_._2 != null).toSet;
        pr = context.probeMap.unpack(s.probe);
        relSamples = meta.samples.filter(x => const.subsetOf(x.sampleClass.constraints.toSet))) {

      val present = for (
        s <- relSamples;
        ev <- data.asExtValues(s).get(pr);
        if (ev.present)
      ) yield s

       val expectedTimes = present.flatMap(x => meta.parameter(x, "exposure_time"))
      s.points.map(_.code) should contain theSameElementsAs(expectedTimes.map(timeMap(_)))
    }

    for (s <- TestData.samples) {
      val x = OTGSeries.buildEmpty(s, meta)
      ss.exists(_.classCode == x.classCode) should be(true)
    }

    val xs = TestData.samples.map(x => OTGSeries.buildEmpty(x, meta))
    for (s <- ss) {
      xs.exists(_.classCode == s.classCode) should be(true)
    }

    ss.map(s => (s.probe, s.classCode)).toSeq.distinct should
      equal(ss.map(s => (s.probe, s.classCode)))
  }
}
