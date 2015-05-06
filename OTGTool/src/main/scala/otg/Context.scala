package otg

import otg.db.Metadata
import otg.sparql.OTGSamples
import t.TriplestoreConfig
import t.db.MatrixContext
import t.db.MatrixDBReader
import t.db.ProbeIndex
import t.db.kyotocabinet.KCMatrixDB
import t.db.SampleIndex
import t.BaseConfig
import t.db.PExprValue
import t.db.ExprValue
import t.db.ProbeMap
import t.sparql.Samples
import otg.sparql.Probes
import t.db.TRefresher
import t.db.SeriesBuilder

object Context {
  val factory = new Factory()
  
  def apply(bc: OTGBConfig) =
     new otg.Context(bc, factory, 
        factory.probes(bc.triplestore), factory.samples(bc),
        new OTGContext(bc, None))          
}

//TODO consider adding a sparql subcontext with lazy val endpoints
class Context(override val config: OTGBConfig, 
    override val factory: Factory, 
    override val probes: Probes, 
    override val samples: OTGSamples,
    override val matrix: OTGContext) 
  extends t.Context(config, factory, probes, samples, matrix)

/**
 * TODO: split up properly/rename
 */
class OTGContext(baseConfig: BaseConfig,  
   /**
   * Metadata for samples. Not always available.
   * Only intended for use during maintenance operations.
   */
    val metadata: Option[Metadata] = None) extends MatrixContext {

  private val otgHomeDir = baseConfig.data.dir     
  private val data = baseConfig.data
  private val maps = new TRefresher(baseConfig)
  
  def triplestoreConfig: TriplestoreConfig = baseConfig.triplestore
  
  def probeMap = maps.latest.unifiedProbes
  
  def enumMaps = maps.latest.enumMaps
  def sampleMap = maps.latest.sampleMap
  
  lazy val samples: OTGSamples =     
    new OTGSamples(baseConfig)
  
  /**
   * Obtain a reader for the absolute value/normalized intensity
   *  database.
   */
  def absoluteDBReader: MatrixDBReader[ExprValue] = 
    KCMatrixDB(data.exprDb, false)(this)    

  /**
   * Obtain a reader for the folds database.
   */
  def foldsDBReader: MatrixDBReader[PExprValue] = 
    KCMatrixDB.applyExt(data.foldDb, false)(this)
    
  def seriesBuilder: OTGSeries.type = OTGSeries
    
  //TODO add series DB reader
      
  var testRun: Boolean = false
  
  def close() {    
    samples.close()
  }
}