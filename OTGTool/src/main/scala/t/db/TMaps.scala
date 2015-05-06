package t.db

import t.BaseConfig
import t.Refreshable
import t.db.kyotocabinet.KCIndexDB

//TODO: this is arguably a KyotoCabinet specific concept.
class TMaps(config: BaseConfig) {
  def data = config.data
  
  lazy val unifiedProbes: ProbeMap =     
    new ProbeIndex(KCIndexDB.readOnce(data.probeIndex))
  
  lazy val sampleMap =     
    new SampleIndex(KCIndexDB.readOnce(data.sampleIndex))
  
  lazy val enumMaps = {
    val db = KCIndexDB(data.enumIndex, false)
    try {
      db.enumMaps(config.seriesBuilder.enums)
    } finally {
      db.release
    }
  }
}

class TRefresher(config: BaseConfig) extends Refreshable[TMaps] {

  def currentTimestamp: Long = {
    //If this file has been updated, we reload all the maps
    new java.io.File(config.data.exprDb).lastModified()
  }

  def reload: TMaps = new TMaps(config)   
}