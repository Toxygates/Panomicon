/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

/**
 * Statistics functions that are safe to use when there may be missing data.
 */
object SafeMath {

  private def filtered(vs: Iterable[Double]) =
    vs.filter(x => !java.lang.Double.isNaN(x) &&
            java.lang.Double.isFinite(x))

//  def removeNaNValues(vs: Iterable[Double]) =
//    vs.filter(!java.lang.Double.isNaN(_))

  private def safely[T](vs: Iterable[Double],
    f: Iterable[Double] => Double): Double = {
    val fvs = filtered(vs)
    if (fvs.isEmpty) Double.NaN else f(fvs)
  }

  def safeProduct(vs: Iterable[Double]) =
    safely(vs, _.product)

  def safeMax(vs: Iterable[Double]) =
    safely(vs, _.max)

  def safeMin(vs: Iterable[Double]) =
    safely(vs, _.min)

  def safeMean(vs: Iterable[Double]) =
    safely(vs, fs => fs.sum / fs.size)

  def safeSum(vs: Iterable[Double]) =
    safely(vs, _.sum)

  def safeSigma(vs: Iterable[Double]) =
    safely(vs, Statistics.sigma(_))

  def safeIsGreater(x: Double, y: Double) = {
    if (java.lang.Double.isNaN(x)) false
    else if (java.lang.Double.isNaN(y)) true
    else (x > y)
  }
}
