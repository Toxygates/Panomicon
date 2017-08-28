package t.db

import t.model.sample.Attribute

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