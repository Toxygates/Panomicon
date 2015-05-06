package t.db

import friedrich.util.formats.TSVFile

case class SampleParameter(identifier: String, humanReadable: String)

trait ParameterSet {
  def all: Iterable[SampleParameter]
  def required: Iterable[SampleParameter]
  def highLevel: Iterable[SampleParameter]
  lazy val byId = Map() ++ all.map(x => x.identifier -> x)
  lazy val byIdLowercase = byId.map(x => x._1.toLowerCase() -> x._2)   
}

trait Metadata {
 def samples: Iterable[Sample]
    
  def parameters(s: Sample): Iterable[(SampleParameter, String)]
  
  def parameterMap(s: Sample): Map[String, String] =
    Map() ++ parameters(s).map(x => x._1.identifier -> x._2)
  /**
   * Obtain all available values for a given parameter.
   */
  def parameterValues(identifier: String): Set[String]
  
  def parameter(s: Sample, identifier: String): String = 
    parameterMap(s)(identifier)
    
  /**
   * Does this metadata set have information about the given sample?
   */
  def contains(s: Sample): Boolean = !parameters(s).isEmpty
  
  def platform(s: Sample): String = parameter(s, "platform_id")
  
  def isControl(s: Sample): Boolean = false 
   
  /**
   * Retrieve the set of control samples corresponding to a given sample.
   */
  def controlSamples(s: Sample): Iterable[Sample] = List()
}

