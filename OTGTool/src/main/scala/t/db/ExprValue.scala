/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.db
import java.text.NumberFormat
import t.platform.Probe
import t.platform.SimpleProbe

/**
 * TODO: ExprValue should move to some subpackage of t
 */
object ExprValue {
  def presentMean(vs: Iterable[ExprValue], probe: String): ExprValue = {
    val nps = vs.filter(_.call != 'A')
    if (nps.size > 0) {
      apply(nps.map(_.value).sum / nps.size, 'P', probe)
    } else {
      apply(0, 'A', probe)
    }
  }

  def allMean(vs: Iterable[ExprValue], probe: String): ExprValue = {
    val value = if (vs.size > 0) {
      vs.map(_.value).sum / vs.size
    } else {
      0
    }

    val nps = vs.filter(_.call != 'A')
    val call = if (nps.size > vs.size / 2) {
      'P'
    } else {
      'A'
    }

    apply(value, call, probe)
  }

  def apply(v: Double, call: Char = 'P', probe: String = null) = BasicExprValue(v, call, probe)

  val nf = NumberFormat.getNumberInstance()

  def isBefore(v1: ExprValue, v2: ExprValue): Boolean =
    (v1.call, v2.call) match {
      case ('A', 'M') => true
      case ('A', 'P') => true
      case ('M', 'A') => false
      case ('M', 'P') => true
      case ('P', 'A') => false
      case ('P', 'M') => true
      case _ => v1.value < v2.value
    }

}

trait ExprValue {
  def value: Double
  def call: Char
  def probe: String

  // Currently we interpret both P and M as present
  def present: Boolean = (call != 'A')

  override def toString(): String =
    s"(${ExprValue.nf.format(value)}:$call)"
}

/**
 * An expression value for one probe in a microarray sample, with an associated
 * call. Call can be P, A or M (present, absent, marginal).
 */
case class BasicExprValue(value: Double, call: Char = 'P', probe: String = null) extends ExprValue

/**
 * An expression value that also has an associated p-value.
 * The p-value is associated with fold changes in the smallest
 * sample group that the sample belongs to (all samples with identical
 * experimental conditions).
 */
case class PExprValue(value: Double, p: Double, call: Char = 'P', probe: String = null) extends ExprValue {
    override def toString(): String =
      s"(${ExprValue.nf.format(value)}:$call:${ExprValue.nf.format(p)})"
}
