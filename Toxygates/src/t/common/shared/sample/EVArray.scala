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

package t.common.shared.sample

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.collection.mutable.ArrayBuilder

object EVABuilder extends CanBuildFrom[Seq[ExpressionValue], ExpressionValue, EVArray] {
  def apply() = new EVABuilder

  def apply(s: Seq[ExpressionValue]): EVABuilder =
    new EVABuilder
}

class EVABuilder(_values: Seq[Double] = Seq(),
    _calls: Seq[Char] = Seq(),
    _tooltips: Seq[String] = Seq()) extends Builder[ExpressionValue, EVArray] {

  private var values = new ArrayBuilder.ofDouble ++= _values
  private var calls = new ArrayBuilder.ofChar ++= _calls
  private var tooltips = new ArrayBuilder.ofRef[String] ++= _tooltips

  def clear(): scala.Unit = {
    values.clear()
    calls.clear()
    tooltips.clear()
  }

  def +=(x: ExpressionValue): this.type = {
    values += x.getValue
    calls += x.getCall
    tooltips += x.getTooltip
    this
  }

  override def sizeHint(h: Int): scala.Unit = {
    values.sizeHint(h)
    calls.sizeHint(h)
    tooltips.sizeHint(h)
  }

  def result = new EVArray(values.result, calls.result, tooltips.result)
}

object EVArray {
  def apply(evs: Seq[ExpressionValue]): EVArray =
    (new EVABuilder ++= evs).result
}

//TODO For scalability, think about avoiding the tooltips
//since they are not shared, and not primitives
class EVArray(values: Array[Double],
    calls: Array[Char],
    tooltips: Array[String]) extends Seq[ExpressionValue] {

  def apply(i: Int): ExpressionValue =
    new ExpressionValue(values(i), calls(i), tooltips(i))

  def length: Int = values.length

  def iterator: Iterator[ExpressionValue] = (0 until length).map(apply).iterator
}
