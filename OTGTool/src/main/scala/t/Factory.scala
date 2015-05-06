package t
import t.sparql._
import t.db.file.TSVMetadata
import t.db.ParameterSet

//TODO consider making ts, data constructor parameters
//then store them and the resulting context, baseconfig
abstract class Factory {
  def samples(config: BaseConfig): Samples
  
  def probes(config: TriplestoreConfig): Probes
 
  def tsvMetadata(file: String): TSVMetadata 
  
  def context(ts: TriplestoreConfig, data: DataConfig): Context  
}