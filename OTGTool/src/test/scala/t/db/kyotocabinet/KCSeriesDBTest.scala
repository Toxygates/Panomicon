package t.db.kyotocabinet

import otg._
import t.TTestSuite
import t.db.testing.TestData

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
    val time = TestData.enumMaps("exposure_time")("9 hr") //nonexistent in default test data

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
    val time = TestData.enumValues("exposure_time").head

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
