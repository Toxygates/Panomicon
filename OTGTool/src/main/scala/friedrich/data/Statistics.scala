/**
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Part of the Friedrich bioinformatics framework.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */
package friedrich.data

/**
 * General statistics functions.
 */
object Statistics {

  import scala.language.implicitConversions

  /**
   * Compute the arithmetic mean.
   */
  def mean(data: Iterable[Double]): Double =
    if (data.size == 0) { 0 } else { data.sum / data.size }

  /**
   * Compute the geometric mean.
   */
  def geomean(data: Iterable[Double]): Double =
    if (data.size == 0) { 1 } else { Math.pow(data.product, 1.0 / data.size) }

  /**
   * Compute sample standard deviation.
   */
  def sigma(data: Iterable[Double]): Double = {
    val mn = mean(data)
    val terms = data.map(x => (x - mn) * (x - mn))
    Math.sqrt(1.0 / (data.size - 1) * terms.sum)
  }

  /**
   * Squares of all samples
   */
  def square(data: Iterable[Double]) = data.map(x => x * x)

  /**
   * Square root
   */
  def sqrt(v: Double): Double = Math.sqrt(v)

  /**
   * Square roots of all samples
   */
  def sqrt(data: Iterable[Double]): Iterable[Double] = data.map(sqrt)

  def abs(v: Double) = Math.abs(v)

  /**
   * Compute the Pearson correlation coefficient.
   */
  def pearsonCorrelation(d1: Seq[Double], d2: Seq[Double]): Double = {
    assert(d1.size == d2.size)
    val n = d1.size
    val m1 = mean(d1)
    val m2 = mean(d2)
    val std1 = sigma(d1)
    val std2 = sigma(d2)
    val terms = d1.zip(d2).map(p => { (p._1 - m1) / std1 * (p._2 - m2) / std2 })
    1.0 / (n - 1.0) * terms.sum
  }
}
