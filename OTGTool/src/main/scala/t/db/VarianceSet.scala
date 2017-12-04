package t.db

import t.model.sample.Attribute
import org.apache.commons.math3.stat.StatUtils.variance
import org.apache.commons.math3.stat.StatUtils.mean
import scala.collection.mutable.HashMap

/**
 * Provides distributional information for attributes, typically from a
 * control group or other set of samples.
 */
abstract class VarianceSet() {

  def varAndMean(param: Attribute): Option[(Double, Double)]

  def lowerBound(param: Attribute, zTestSampleSize: Int): Option[Double] =
    varAndMean(param).map {
      case (v, m) =>
        val sd = Math.sqrt(v)
        m - 2 / Math.sqrt(zTestSampleSize) * sd
    }

  def upperBound(param: Attribute, zTestSampleSize: Int): Option[Double] =
    varAndMean(param).map {
      case (v, m) =>
        val sd = Math.sqrt(v)
        m + 2 / Math.sqrt(zTestSampleSize) * sd
    }
}

/**
 * VarianceSet that simply uses the parameter values stored in a sample object
 */
class SimpleVarianceSet(samples: Iterable[t.model.sample.SampleLike]) extends VarianceSet {

  val varsAndMeans = new HashMap[Attribute, Option[(Double, Double)]]

  def varAndMean(param: Attribute): Option[(Double, Double)] = {
    varsAndMeans.get(param) match {
      case Some(somePair) =>
        somePair
      case None =>
        val attributeValues = samples.flatMap(Sample.numericalValue(_, param)).toArray
        val newPair = if (attributeValues.size < 2) {
          None
        } else {
          Some((variance(attributeValues), mean(attributeValues)))
        }
        varsAndMeans.put(param, newPair)
        newPair
    }
  }
}
