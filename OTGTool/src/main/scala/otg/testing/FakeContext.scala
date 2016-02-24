package otg.testing

import t.db.SampleMap
import t.db.ProbeMap
import otg.OTGSeries
import t.db.kyotocabinet.KCSeriesDB

class FakeContext(sampleMap: SampleMap, probeMap: ProbeMap,
  enumMaps: Map[String, Map[String, Int]] = Map())
  extends t.testing.FakeContext(sampleMap, probeMap, enumMaps) {

  val seriesDB = t.db.testing.TestData.memDBHash
  override def seriesBuilder = OTGSeries
  override def seriesDBReader = new KCSeriesDB(seriesDB, false, seriesBuilder, true)(this)
}
