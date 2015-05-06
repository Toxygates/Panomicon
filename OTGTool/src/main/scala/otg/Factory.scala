package otg

import t.TriplestoreConfig
import t.sparql.Samples
import otg.sparql.OTGSamples
import otg.sparql.Probes
import t.BaseConfig
import otg.db.file.TSVMetadata
import t.DataConfig

class Factory extends t.Factory {
  override def samples(config: BaseConfig): OTGSamples = 
    new OTGSamples(config)
  
  override def probes(config: TriplestoreConfig): Probes =
    new Probes(config)
  
  override def tsvMetadata(file: String) = new TSVMetadata(file)
  
  override def context(ts: TriplestoreConfig, data: DataConfig) = {
    val bc = new OTGBConfig(ts, data)
    otg.Context(bc)
  }    
}