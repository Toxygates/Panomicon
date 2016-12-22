package t.platform

import t.db.SampleParameter

case class BioParameter(key: String, label: String, kind: String,
    section: Option[String],
    lowerBound: Option[Double], upperBound: Option[Double],
    attributes: Map[String, String] = Map()) {

  /**
   * Obtain an equivalent SampleParameter (for lookup purposes)
   */
  def sampleParameter = SampleParameter(key, label)
}

class BioParameters(lookup: Map[String, BioParameter]) {
  def apply(key: String) = lookup(key)
  def get(key: String) = lookup.get(key)

  def sampleParameters = lookup.values.map(_.sampleParameter)

  /**
   * Extract bio parameters with accurate low and high threshold for a given
   * time point. The raw values are stored in the root parameter set's
   * attribute maps.
   * @param time The time point, e.g. "24 hr"
   */
  def forTimePoint(time: String): BioParameters = {
    val normalTime = time.replaceAll("\\s+", "")

    new BioParameters(Map() ++
      (for (
        (id, param) <- lookup;
        lb = param.attributes.get(s"lowerBound_$normalTime").
          map(_.toDouble).orElse(param.lowerBound);
        ub = param.attributes.get(s"upperBound_$normalTime").
          map(_.toDouble).orElse(param.upperBound);
        edited = BioParameter(id, param.label, param.kind, param.section, lb, ub,
          param.attributes)
      ) yield (id -> edited)))
  }
}
