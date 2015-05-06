package t.testing

import t.db.Metadata
import otg.Species._
import t.db.ProbeMap
import t.db.SampleMap
import t.db.MatrixContext
import t.db.MatrixDBReader
import t.db.PExprValue
import t.db.ExprValue
import t.db.SeriesBuilder

class FakeContext(val sampleMap: SampleMap, val probeMap: ProbeMap, 
    val enumMaps: Map[String, Map[String, Int]] = Map()) extends MatrixContext {

  def species = List(Rat)

  def probes(s: Species): ProbeMap = probeMap
  def unifiedProbes = probeMap
  
  /**
   * Metadata for samples. Not always available.
   * Only intended for use during maintenance operations.
   */
  def metadata: Option[Metadata] = None
  
  def samples = null //TODO
    
  def absoluteDBReader: MatrixDBReader[ExprValue] = null //TODO
  def foldsDBReader: MatrixDBReader[PExprValue] = null //TODO
  def seriesBuilder: SeriesBuilder[_] = null //TODO
}