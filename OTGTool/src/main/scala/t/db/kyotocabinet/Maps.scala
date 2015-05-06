package t.db.kyotocabinet

import t.DataConfig
import t.db.ProbeIndex
import t.db.ProbeMap
import t.db.SampleIndex

class Maps(data: DataConfig) {    
  
  lazy val unifiedProbes: ProbeMap =     
    new ProbeIndex(KCIndexDB.readOnce(data.probeIndex))
  
  lazy val sampleMap =     
    new SampleIndex(KCIndexDB.readOnce(data.sampleIndex))  
}