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
  var db: SeriesDB[OTGSeries] = _

  val config = TestConfig.config
  implicit val context = new otg.testing.FakeContext(TestData.dbIdMap,
    TestData.probeMap, otg.testing.TestData.enumMaps)

  //TODO change
  val pmap = context.probeMap
  val cmap = context.enumMaps("compound_name")

  before {
    // TODO change the way this is configured
    //    System.setProperty("otg.home", "/Users/johan/otg/20120221/open-tggates")
    db = context.seriesDBReader
  }

  def writer() = new KCSeriesDB(context.seriesDB, true, OTGSeries, true)(context)

  test("Series retrieval") {
    val packed = pmap.pack("probe_1")

    for (c <- cmap.keys) {
      val key = OTGSeries("Single", "Liver", "Rat", packed, c, null, null, Seq())
      val ss = db.read(key)
      if (!ss.isEmpty) {
        val s = ss.head
        println(s)
        s.compound should equal(c)
        s.probe should equal(packed)
      }
    }
  }

  test("Series retrieval with blank compound") {
    val packed = pmap.pack("probe_1")
    def checkCombination(repeat: String, organ: String, organism: String, expectedCount: Int) {
      val key = OTGSeries(repeat, organ, organism, packed, null, null, null, Seq())
      val ss = db.read(key)
      val n = ss.map(_.compound).toSet.size
      println(s"$repeat/$organ/$organism: $n")
      assert(n === expectedCount)
    }

    // These counts are valid for otg-test (adjuvant version)
    checkCombination("Single", "Liver", "Rat", 162)
    checkCombination("Repeat", "Liver", "Rat", 143)
    checkCombination("Single", "Kidney", "Rat", 43)
    checkCombination("Repeat", "Kidney", "Rat", 41)

    //Note that "Vitro" is treated as an organ here
    checkCombination("Single", "Vitro", "Rat", 145)
    checkCombination("Single", "Vitro", "Human", 158)
  }

  after {
    db.release()
  }
}
