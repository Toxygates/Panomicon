/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import t.model.SampleClass
import t.model.shared.SampleClassHelper
import t.model.sample.Attribute
import t.model.sample.SampleLike

/**
 * A sample.
 */
case class Sample(sampleId: String, sampleClass: SampleClass) {

  def dbCode(implicit context: MatrixContext): Int =
    context.sampleMap.pack(sampleId)
    
  def getDbCode(implicit context: MatrixContext): Option[Int] = {
    if (context.sampleMap.isToken(sampleId)) {
      Some(context.sampleMap.pack(sampleId))
    } else {
      None
    }
  }

  def identifier = sampleId

  override def hashCode: Int = sampleId.hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case s: Sample => sampleId == s.sampleId
      case _         => false
    }
  }

  override def toString = sampleId

  /**
   * Convenience method to obtain a parameter from the sample class.
   */
  def get(key: Attribute): Option[String] = Option(sampleClass.get(key))

  /**
   * Convenience method to obtain a parameter from the sample class.
   */
  def apply(key: Attribute): String = sampleClass(key)
}

object Sample {
  def identifierFor(code: Int)(implicit context: MatrixContext): String = {
    context.sampleMap.tryUnpack(code) match {
      case Some(i) => i
      case None =>
        val r = s"unknown_sample[$code]"
        println(r)
        r
    }
  }

  def numericalValue(sample: SampleLike, key: Attribute): Option[Double] = {
    if (key.isNumerical) {
      Option(sample.get(key)) match {
        case Some(v) => try {
          Some(v.toDouble)
        } catch {
          case e: NumberFormatException => None
        }
        case None    => None
      }
    } else {
      throw new IllegalArgumentException(s"Tried to get numerical value of non-numerical attribute $key")
    }
  }

  def apply(code: Int)(implicit context: MatrixContext): Sample = {
    new Sample(identifierFor(code), SampleClassHelper())
  }

  def apply(id: String) = new Sample(id, SampleClassHelper())

  def apply(id: String, map: Map[Attribute, String]) = new Sample(id, SampleClassHelper(map))
}
