package otg.db

import otg.Annotation
import t.db.Sample
import t.db.ParameterSet

/**
 * Information about a set of samples.
 */
trait Metadata extends t.db.Metadata {
  
  override def isControl(s: Sample): Boolean = parameter(s, "dose_level") == "Control"
 
  /**
   * Retrieve the set of compounds that exist in this metadata set.
   */
  def compounds: Set[String] = parameterValues("compound_name")
  
  /**
   * Retrieve the compound associated with a particular sample.
   */
  def compound(s: Sample): String = parameter(s, "compound_name")    
  
}

object SampleParameter extends ParameterSet {  
  val all =
    Annotation.keys.map(x => t.db.SampleParameter(x._2, x._1)) ++ 
    List(t.db.SampleParameter("sample_id", "Sample ID"), 
        t.db.SampleParameter("control_group", "Control group"))
  
  val highLevel = List("organism", "test_type", "sin_rep_type", "organ_id").map(byId)
  
  val required = highLevel ++ List("sample_id", 
      "compound_name", "dose_level", "exposure_time", 
      "platform_id", "control_group").map(byId)

}