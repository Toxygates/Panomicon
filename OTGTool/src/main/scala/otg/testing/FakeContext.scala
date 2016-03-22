package otg.testing

import t.db.SampleMap
import t.db.ProbeMap
import otg.OTGSeries
import t.db.kyotocabinet.KCSeriesDB
import t.db.testing.{TestData => TData}

class FakeContext(sampleMap: SampleMap = TData.dbIdMap,
    probeMap: ProbeMap = TData.probeMap,
    enumMaps: Map[String, Map[String, Int]] = TData.enumMaps)
  extends t.testing.FakeContext(sampleMap, probeMap, enumMaps) {

  val seriesDB = t.db.testing.TestData.memDBHash
  override def seriesBuilder = OTGSeries
  override def seriesDBReader = new KCSeriesDB(seriesDB, false, seriesBuilder, true)(this)
}
