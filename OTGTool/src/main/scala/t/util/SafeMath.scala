/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.util

import friedrich.data.Statistics

import scala.collection.TraversableLike

/**
 * Statistics functions that are safe to use when there may be missing data.
 */
object SafeMath {

  private def valueFilter(x: Double) = !java.lang.Double.isNaN(x) &&
      java.lang.Double.isFinite(x)

  private def filtered(vs: List[Double]): List[Double] = vs.filter(valueFilter)

  private def filtered(vs: TraversableOnce[Double]): TraversableOnce[Double] = vs.filter(valueFilter)

  private def safely(vs: List[Double], f: List[Double] => Double): Double = {
    val fvs = filtered(vs)
    if (fvs.isEmpty) Double.NaN else f(fvs)
  }

  private def safely(vs: TraversableOnce[Double], f: TraversableOnce[Double] => Double): Double = {
    val fvs = filtered(vs)
    if (fvs.isEmpty) Double.NaN else f(fvs)
  }

  def safeProduct(vs: TraversableOnce[Double]): Double = safely(vs, _.product)

  def safeMax(vs: TraversableOnce[Double]): Double = safely(vs, _.max)

  def safeMin(vs: TraversableOnce[Double]): Double = safely(vs, _.min)

  val l2 = Math.log(2)
  /**
   * Safely compute the mean of the values
   * @param vs
   * @param pow2 Whether to transform each value x into 2^x before computing the mean, and then take the log2 again
   * @return
   */
  def safeMean(vs: TraversableOnce[Double], pow2: Boolean = false): Double = {
    val valid = vs.filter(valueFilter)
    var sum = 0.0
    var size = 0
    if (valid.isEmpty) {
      return Double.NaN
    }

    if (pow2) {
      for { x <- valid } {
        sum += Math.pow(2, x)
        size += 1
      }
      Math.log(sum / size) / l2
    } else {
      for { x <- valid } {
        sum += x
        size += 1
      }
      sum/size
    }
  }

  def safeSum(vs: TraversableOnce[Double]): Double = safely(vs, _.sum)

  def safeSigma(vs: List[Double]): Double = safely(vs, Statistics.sigma(_: List[Double]))

  def safeIsGreater(x: Double, y: Double): Boolean = {
    if (java.lang.Double.isNaN(x)) false
    else if (java.lang.Double.isNaN(y)) true
    else (x > y)
  }
}
