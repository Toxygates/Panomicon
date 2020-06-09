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

package t.db.kyotocabinet

import kyotocabinet.DB
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.DBTestData
import t.model.sample.OTGAttribute._
import t.testing.FakeContext
import t._

@RunWith(classOf[JUnitRunner])
class KCSeriesDBTest extends TTestSuite {
  import t.testing.{TestData => OData}
  implicit var context: FakeContext = _
  def cmap = context.enumMaps("compound_name")

  trait seriesTestType {
    def name: String
    def seriesType: OTGSeriesType
    def builderType: OTGSeriesBuilder
    def storageDB: DB

    def inputSeries: Iterable[OTGSeries]
    def attributeValue: String
    def attribute = seriesType.independentVariable
    
    def keyFraction: Double
    
    def normalizingReader() = new KCSeriesDB(storageDB, false, builderType, true)(context)
    def nonNormalizingReader() = new KCSeriesDB(storageDB, false, builderType, false)(context)
    def writer() = new KCSeriesDB(storageDB, true, builderType, false)(context)
  }

  object timeSeriesTest extends seriesTestType {
    val name = "time series"
    val seriesType = TimeSeries
    val builderType = OTGTimeSeriesBuilder
    def storageDB = context.timeSeriesDB
    val attributeValue = OData.absentTime
    
    val keyFraction = OData.usedDosePoints.size.toDouble / 
      DBTestData.enumValues(DoseLevel.id).size
    val inputSeries = OData.series
  }

  object doseSeriesTest extends seriesTestType {
    val name = "dose series"
    val seriesType = DoseSeries
    val builderType = OTGDoseSeriesBuilder
    def storageDB = context.doseSeriesDB
    val attributeValue = OData.absentDose
    
    val keyFraction = OData.usedTimePoints.size.toDouble / 
      DBTestData.enumValues(ExposureTime.id).size
    val inputSeries = OData.doseSeries
  }

  val testTypes = List(timeSeriesTest, doseSeriesTest)

  before {
    context = new FakeContext()
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

      var key = t.OTGSeries(testType.seriesType, null, null, null, 100, compound, null, null)
      var nExpected = testType.inputSeries.size / cmap.size / DBTestData.probes.size
      val builtKeys = testType.builderType.keysFor(key)      
      (builtKeys.size * testType.keyFraction) should equal(nExpected)

      var ss = db.read(key)
      ss.size should equal(nExpected)
      var expect = testType.inputSeries.filter(s => s.compound == compound && s.probe == 100)
      expect.size should equal(ss.size)
      ss should contain theSameElementsAs(expect)

      val organ = DBTestData.enumValues(Organ.id).head
      key = t.OTGSeries(testType.seriesType, null, organ, null, 13, compound, null, null)
      nExpected = nExpected / DBTestData.enumValues(Organ.id).size
      expect = testType.inputSeries.filter(s => s.compound == compound && s.probe == 13 && s.organ == organ)
      ss = db.read(key)
      ss.size should equal(nExpected)
      ss should contain theSameElementsAs(expect)
    }
  }

  for (testType <- testTypes) {
    test("Point insertion - " + testType.name) {
      val compound = cmap.keys.head
      val testProbe = 100
      val probe = context.probeMap.unpack(testProbe)

      val attribValuePacked = DBTestData.enumMaps(testType.attribute.id())(testType.attributeValue)

      val baseSeries = testType.inputSeries.filter(s => s.compound == compound && 
        s.probe == testProbe)

      val (expected, insertionData) = baseSeries.toSeq.map(s => {
        val np = OData.mkPoint(probe, attribValuePacked) 
        (s.copy(points = ((s.points :+ np).sortBy(_.code))), s.copy(points = Seq(np)))
      }).unzip

      val w = testType.writer()
      for (s <- insertionData) {
        w.addPoints(s)
      }
      val key = t.OTGSeries(testType.seriesType, null, null, null, testProbe, compound, null, null)
      val db = testType.normalizingReader()
      var ss = db.read(key)
      ss should contain theSameElementsAs (expected)
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

      var key = t.OTGSeries(testType.seriesType, null, null, null, 100, null, null, null)
      var expect = testType.inputSeries.filter(s => s.compound != compound && s.probe == 100)
      var ss = w.read(key)
      ss should contain theSameElementsAs (expect)
    }
  }

  for (testType <- testTypes) {
    test("Point deletion - " + testType.name) { 
      val compound = cmap.keys.head
      val testProbe = 100
      val attribValuePacked = DBTestData.enumMaps(testType.attribute.id())(testType.attributeValue)

      var del = testType.inputSeries.filter(s => s.compound == compound && 
        s.probe == testProbe)
      val w = testType.writer()
      for (s <- del) {
        w.removePoints(s.copy(points = s.points.filter(_.code == attribValuePacked)))
      }

      var key = t.OTGSeries(testType.seriesType, null, null, null, 100, compound, null, null)
      var expect = testType.inputSeries.filter(s => s.compound == compound && 
        s.probe == testProbe).map(s =>
          s.copy(points = s.points.filter(_.code != attribValuePacked)))
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
  
  test("Equivalence of time and dose series") {    
    
    //Re-shuffle the time series as dose series to get consistent data
    val dwriter = doseSeriesTest.writer()
    for ((g, ss) <- OData.series.groupBy(s => 
      (s.compound, s.organ, s.organism, s.probe, s.repeat, s.testType));
      t <- OData.usedTimePoints) {
      val dpoints = ss.map(s => s.points.filter(_.code == t._2).
        head.copy(code = DBTestData.enumMaps(DoseLevel.id)(s.doseOrTime)))
      val dseries = ss.head.copy(seriesType = DoseSeries, points = dpoints,
        doseOrTime = t._1)
      dwriter.addPoints(dseries)      
    }
    
    val treader = timeSeriesTest.nonNormalizingReader
    val dreader = doseSeriesTest.nonNormalizingReader
    
    for (s <- OData.series;
      key = s.copy(points = Seq(), doseOrTime = null);
      tdata = treader.read(key);
      ddata = dreader.read(key)) {

      assert(tdata.size > 0)
      assert(ddata.size > 0)
      
      val tpoints = tdata.flatMap(_.points.map(_.value))
      val dpoints = ddata.flatMap(_.points.map(_.value))
      
      assert (tpoints.size > 0)
      tpoints should contain theSameElementsAs (dpoints)
    }   
  }
}
