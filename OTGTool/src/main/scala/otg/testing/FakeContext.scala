package otg.testing

import t.db.SampleMap
import t.db.ProbeMap
import t.db.testing.TestData
import otg.OTGSeries
import t.db.kyotocabinet.KCSeriesDB

class FakeContext(sampleMap: SampleMap, probeMap: ProbeMap,
  enumMaps: Map[String, Map[String, Int]] = Map())
  extends t.testing.FakeContext(sampleMap, probeMap, enumMaps) {

  import TestData._

  private val ser = memDBHash
  override def seriesBuilder = OTGSeries
  override def seriesDBReader = new KCSeriesDB(ser, false, seriesBuilder, true)(this)
}
