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

package t.db.kyotocabinet

import otg._
import t.TTestSuite
import t.db.testing.TestData
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.SampleParameters._
import otg.model.sample.Attribute.ExposureTime

@RunWith(classOf[JUnitRunner])
class KCSeriesDBTest extends TTestSuite {
  var db: SDB = _

  import otg.testing.{TestData => OData}
  implicit var context: otg.testing.FakeContext = _
  def cmap = context.enumMaps("compound_name")

  before {
    context = new otg.testing.FakeContext()
    //this is normalizing by default
    db = context.seriesDBReader
    var w: SDB = null
    try {
      w = writer()
      println(s"Insert ${OData.series.size} series")
      for (s <- OData.series) {
        w.addPoints(s)
      }

    } finally {
      w.release
    }
  }

  after {
    db.release()
  }

  def nonNormalisingReader() = new KCSeriesDB(context.seriesDB, false, OTGSeries, false)(context)
  def writer() = new KCSeriesDB(context.seriesDB, true, OTGSeries, false)(context)

  test("Series retrieval") {
    val db = nonNormalisingReader()
    val compound = cmap.keys.head

    var key = OTGSeries(null, null, null, 100, compound, null, null)
    var nExpected = OData.series.size / cmap.size / TestData.probes.size
    OTGSeries.keysFor(key).size should equal(nExpected)

    var ss = db.read(key)
    ss.size should equal(nExpected)
    var expect = OData.series.filter(s => s.compound == compound && s.probe == 100)
    expect.size should equal(ss.size)
    ss should contain theSameElementsAs(expect)

    val organ = TestData.enumValues("organ_id").head
    key = OTGSeries(null, organ, null, 13, compound, null, null)
    nExpected = nExpected / TestData.enumValues("organ_id").size
    expect = OData.series.filter(s => s.compound == compound && s.probe == 13 && s.organ == organ)
    ss = db.read(key)
    ss.size should equal(nExpected)
    ss should contain theSameElementsAs(expect)

    db.release()
  }

  test("insert points") {
    val compound = cmap.keys.head
    val probe = context.probeMap.unpack(100)
    val time = TestData.enumMaps(ExposureTime.id)("9 hr") //nonexistent in default test data

    val baseSeries = OData.series.filter(s => s.compound == compound && s.probe == 100)

    val (all, ins) = baseSeries.toSeq.map(s => {
      val np = OData.mkPoint(probe, time)
      (s.copy(points = ((s.points :+ np).sortBy(_.code))), s.copy(points = Seq(np)))
    }).unzip

    val w = writer()
    try {
      for (s <- ins) {
        w.addPoints(s)
      }
      val key = OTGSeries(null, null, null, 100, compound, null, null)
      var ss = db.read(key) //normalising reader
      ss should contain theSameElementsAs (all)
    } finally {
      w.release()
    }
  }

  test("delete") {
    val compound = cmap.keys.head
    val del = OData.series.filter(s => s.compound == compound && s.probe == 100)
    val w = writer()
    try {
      for (s <- del) {
        w.removePoints(s)
      }

      var key = OTGSeries(null, null, null, 100, null, null, null)
      var expect = OData.series.filter(s => s.compound != compound && s.probe == 100)
      var ss = w.read(key)
      ss should contain theSameElementsAs (expect)
    } finally {
      w.release
    }
  }

  test("delete points") {
    val compound = cmap.keys.head
    val time = TestData.enumValues(ExposureTime.id).head

    var del = OData.series.filter(s => s.compound == compound && s.probe == 100)
    val w = writer()
    try {
      for (s <- del) {
        w.removePoints(s.copy(points = s.points.filter(_.code == time)))
      }

      var key = OTGSeries(null, null, null, 100, compound, null, null)
      var expect = OData.series.filter(s => s.compound == compound && s.probe == 100).map(s =>
        s.copy(points = s.points.filter(_.code != time)))
      var ss = w.read(key)
      ss should contain theSameElementsAs (expect)

      for (s <- del) {
        //by now, this contains more points than we have in the DB, but this should be fine
        w.removePoints(s)
      }
      ss = w.read(key)
      ss should be(empty)

    } finally {
      w.release
    }
  }
}
