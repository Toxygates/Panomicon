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

package t.db
import java.text.NumberFormat
import t.platform._

object ExprValue {
  import t.util.SafeMath._

  def mean(data: Iterable[ExprValue], presentOnly: Boolean, pow2: Boolean) =
    if (presentOnly) {
      presentMean(data, pow2)
    } else {
      allMean(data, pow2)
    }

  def presentMean(vs: Iterable[ExprValue], pow2: Boolean, probe: ProbeId = ""): BasicExprValue = {
    val present = vs.filter(_.call != 'A')
    val call = if (present.size > 0) 'P' else 'A'
    apply(safeMean(present.map(_.value), pow2), call, probe)
  }

  def allMean(vs: Iterable[ExprValue], pow2: Boolean, probe: ProbeId = ""): BasicExprValue = {
    val value = if (vs.size > 0) {
      safeMean(vs.map(_.value), pow2)
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

  private val l2 = Math.log(2)
  def log2(v: Double): Double = Math.log(v) / l2
  def log2[E <: ExprValue](value: E): BasicExprValue = {
    ExprValue.apply(log2(value.value), value.call, value.probe)
  }


  def apply(v: RawExprValue, call: PACall = 'P', probe: ProbeId = null): BasicExprValue =
    BasicExprValue(v, call, probe)

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
  def value: RawExprValue
  def call: PACall
  def probe: ProbeId

  // Currently we interpret both P and M as present
  def present: Boolean = (call != 'A')

  override def toString(): String =
    s"(${ExprValue.nf.format(value)}:$call)"

  /**
   * Flag to indicate that a missing value was auto-generated as padding by the database.
   */
  var isPadding: Boolean = false

  final def paddingOption = if (isPadding) None else Some(this)
}

/**
 * An expression value for one probe in a microarray sample, with an associated
 * call. Call can be P, A or M (present, absent, marginal).
 */
case class BasicExprValue(value: Double, call: PACall = 'P', probe: ProbeId = null) extends ExprValue

/**
 * An expression value that also has an associated p-value.
 * The p-value is associated with fold changes in the smallest
 * sample group that the sample belongs to (all samples with identical
 * experimental conditions).
 */
case class PExprValue(value: Double, p: PValue, call: PACall = 'P', probe: ProbeId = null) extends ExprValue {
  import java.lang.{Double => JDouble}
  override def toString(): String = {
    if (!JDouble.isNaN(p)) {
      s"(${ExprValue.nf.format(value)}:$call:${ExprValue.nf.format(p)})"
    } else {
      super.toString()
    }
  }

  override def equals(other: Any): Boolean = {
    other match {
      case PExprValue(ov, op, oc, opr) =>
        if (JDouble.isNaN(p) && JDouble.isNaN(op)) {
          value == ov && call == oc && probe == opr
        } else {
          value == ov && call == oc && probe == opr && p == op
        }
      case _ => super.equals(other)
    }
  }
}
