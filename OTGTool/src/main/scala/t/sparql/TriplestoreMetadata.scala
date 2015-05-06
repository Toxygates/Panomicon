package t.sparql

import t.db.Sample
import t.db.SampleParameter
import t.db.Metadata

/**
 * Metadata from a triplestore.
 * The graph to be queried can be influenced by setting
 * os.batchURI.
 */
class TriplestoreMetadata(os: Samples)(implicit sf: SampleFilter) extends Metadata {
	
  /**
   * Retrieve the set of control samples corresponding to a given sample.
   */
  override def controlSamples(s: Sample): Iterable[Sample] = {
    throw new Exception("Implement me")
  }
  
  def samples: Iterable[Sample] = os.samples 
  
  def parameters(s: Sample): Iterable[(SampleParameter, String)] = {
    val annotations = os.annotationQuery(s.identifier, Nil)
    annotations.filter(_._3 != None).map(x => (t.db.SampleParameter(x._2, x._1), x._3.get))
  }
  
  def parameterValues(identifier: String): Set[String] = 
    os.allValuesForSampleAttribute(identifier).toSet  
}
