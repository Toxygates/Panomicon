package t.sample

import t.db.Sample
import t.db.ParameterSet
import t.model.sample.Attribute

/**
 * Fundamental query operations for a set of samples.
 * Sample parameters are identified by SampleParameter and values are strings.
 */
trait SampleSet {
  /**
   * The samples in this sample set.
   */
  def samples: Iterable[Sample]

  lazy val sampleIds = samples.map(_.sampleId).toSet

  /**
   * Obtain all available attributes for a given sample.
   */
  def attributes(sample: Sample): Seq[(Attribute, String)]

  /**
   * Query several sample attributes at once for a given sample.
   * @param querySet the attributes to query, or all if the set is empty.
   */
  def attributes(sample: Sample,
    querySet: Iterable[Attribute]): Seq[(Attribute, String)] = {
    val qs = querySet.toSet
    val ps = attributes(sample)
    if (querySet.isEmpty)
      ps
    else
      ps.filter(p => qs.contains(p._1))
  }

  /**
   * Query a specific attribute for a given sample.
   */
  def attribute(sample: Sample, attrib: Attribute): Option[String] =
    attributes(sample, Seq()).find(_._1 == attrib).map(_._2)

  @deprecated("Query by Attribute instead.", "June 2017")
  def parameter(sample: Sample, parameter: String): Option[String] =
    attributes(sample, Seq()).find(_._1.id == parameter).map(_._2)

}
