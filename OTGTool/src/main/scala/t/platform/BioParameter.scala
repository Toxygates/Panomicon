package t.platform

import t.db.SampleParameter

case class BioParameter(key: String, label: String, kind: String,
    section: Option[String],
    lowThreshold: Option[Double], highThreshold: Option[Double]) {

  /**
   * Obtain an equivalent SampleParameter (for lookup purposes)
   */
  def sampleParameter = SampleParameter(key, label)
}

class BioParameters(lookup: Map[String, BioParameter]) {
  def apply(key: String) = lookup(key)
  def get(key: String) = lookup.get(key)

  def sampleParameters = lookup.values.map(_.sampleParameter)
}
