package t.sample

import t.db.SampleParameter
import t.db.Sample
import t.db.ParameterSet

/**
 * Fundamental query operations for a set of samples.
 * Sample parameters are identified by SampleParameter and values are strings.
 */
trait SampleSet {
  /**
   * The samples in this sample set.
   */
  def samples: Iterable[Sample]

  /**
   * Obtain all available parameters for a given sample.
   */
  def parameters(sample: Sample): Seq[(SampleParameter, String)]

  /**
   * Query several sample parameters at once for a given sample.
   * @param querySet the parameters to query, or all if the set is empty.
   */
  def parameters(sample: Sample,
    querySet: Iterable[SampleParameter]): Seq[(SampleParameter, String)] = {
    val qs = querySet.toSet
    val ps = parameters(sample)
    if (querySet.isEmpty)
      ps
    else
      ps.filter(p => qs.contains(p._1))
  }

  /**
   * Query a specific parameter for a given sample.
   */
  def parameter(sample: Sample, parameter: SampleParameter): Option[String] =
    parameters(sample, Seq()).find(_._1 == parameter).map(_._2)

  @deprecated("Query by SampleParameter instead.", "June 2017")
  def parameter(sample: Sample, parameter: String): Option[String] =
    parameters(sample, Seq()).find(_._1.identifier == parameter).map(_._2)

}
