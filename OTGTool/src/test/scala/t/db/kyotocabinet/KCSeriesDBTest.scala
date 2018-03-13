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
import otg.model.sample.OTGAttribute.ExposureTime
import kyotocabinet.DB

@RunWith(classOf[JUnitRunner])
class KCSeriesDBTest extends TTestSuite {

  import otg.testing.{TestData => OData}
  implicit var context: otg.testing.FakeContext = _
  def cmap = context.enumMaps("compound_name")

  trait seriesTestType {
    def name: String
    def seriesType: OTGSeriesType
    def builderType: OTGSeriesBuilder
    def storageDB: DB

    def inputSeries: Set[OTGSeries]

    def normalizingReader() = new KCSeriesDB(storageDB, false, builderType, true)(context)
    def nonNormalizingReader() = new KCSeriesDB(storageDB, false, builderType, false)(context)
    def writer() = new KCSeriesDB(storageDB, true, builderType, false)(context)
  }

  object timeSeriesTest extends seriesTestType {
    val name = "time series"
    val seriesType = TimeSeries
    val builderType = OTGTimeSeriesBuilder
    def storageDB = context.timeSeriesDB

    val inputSeries = OData.series
  }

  object doseSeriesTest extends seriesTestType {
    val name = "dose series"
    val seriesType = DoseSeries
    val builderType = OTGDoseSeriesBuilder
    def storageDB = context.doseSeriesDB

    val inputSeries = OData.doseSeries
  }

  val testTypes = List(timeSeriesTest, doseSeriesTest)

  before {
    context = new otg.testing.FakeContext()
    for (testType <- testTypes) {
      val w = testType.writer()
      println(s"Insert ${testType.inputSeries.size} series")
      for (s <- testType.inputSeries) {
        w.addPoints(s)
      }
    }
  }

  for (testType <- testTypes) {
    test("Series retrieval - " + testType.name) {
      val db = testType.nonNormalizingReader()
      val compound = cmap.keys.head

      var key = OTGSeries(testType.seriesType, null, null, null, 100, compound, null, null)
      var nExpected = testType.inputSeries.size / cmap.size / TestData.probes.size
      testType.builderType.keysFor(key).size should equal(nExpected)

      var ss = db.read(key)
      ss.size should equal(nExpected)
      var expect = testType.inputSeries.filter(s => s.compound == compound && s.probe == 100)
      expect.size should equal(ss.size)
      ss should contain theSameElementsAs(expect)

      val organ = TestData.enumValues("organ_id").head
      key = OTGSeries(testType.seriesType, null, organ, null, 13, compound, null, null)
      nExpected = nExpected / TestData.enumValues("organ_id").size
      expect = testType.inputSeries.filter(s => s.compound == compound && s.probe == 13 && s.organ == organ)
      ss = db.read(key)
      ss.size should equal(nExpected)
      ss should contain theSameElementsAs(expect)
    }
  }

  for (testType <- testTypes) {
    test("Point insertion - " + testType.name) {
      val compound = cmap.keys.head
      val probe = context.probeMap.unpack(100)
      // needs to be adapted for dose
      val time = TestData.enumMaps(ExposureTime.id)("9 hr") //nonexistent in default test data

      val baseSeries = testType.inputSeries.filter(s => s.compound == compound && s.probe == 100)

      val (all, ins) = baseSeries.toSeq.map(s => {
        val np = OData.mkPoint(probe, time) // needs to be adapted for dose
        (s.copy(points = ((s.points :+ np).sortBy(_.code))), s.copy(points = Seq(np)))
      }).unzip

      val w = testType.writer()
      for (s <- ins) {
        w.addPoints(s)
      }
      val key = OTGSeries(testType.seriesType, null, null, null, 100, compound, null, null)
      val db = testType.normalizingReader()
      var ss = db.read(key)
      ss should contain theSameElementsAs (all)
    }
  }

  for (testType <- testTypes) {
    test("Deletion - " + testType.name) {
      val compound = cmap.keys.head
      val del = testType.inputSeries.filter(s => s.compound == compound && s.probe == 100)
      val w = testType.writer()

      for (s <- del) {
        w.removePoints(s)
      }

      var key = OTGSeries(testType.seriesType, null, null, null, 100, null, null, null)
      var expect = testType.inputSeries.filter(s => s.compound != compound && s.probe == 100)
      var ss = w.read(key)
      ss should contain theSameElementsAs (expect)
    }
  }

  for (testType <- testTypes) {
    test("Point deletion - " + testType.name) { // something in here needs to be adapted for dose
      val compound = cmap.keys.head
      val time = TestData.enumValues(ExposureTime.id).head

      var del = testType.inputSeries.filter(s => s.compound == compound && s.probe == 100)
      val w = testType.writer()
      for (s <- del) {
        w.removePoints(s.copy(points = s.points.filter(_.code == time)))
      }

      var key = OTGSeries(testType.seriesType, null, null, null, 100, compound, null, null)
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
    }
  }
}
