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

package t.db

import t.model.SampleClass
import t.model.shared.SampleClassHelper

/**
 * A sample.
 */
case class Sample(sampleId: String, sampleClass: SampleClass, cgroup: Option[String]) {

  def dbCode(implicit context: MatrixContext): Int =
    context.sampleMap.pack(sampleId)

  def identifier = sampleId

  override def hashCode: Int = sampleId.hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case s: Sample => sampleId == s.sampleId
      case _         => false
    }
  }

  override def toString = sampleId
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

  def apply(code: Int)(implicit context: MatrixContext): Sample = {
    new Sample(identifierFor(code), SampleClassHelper(), None)
  }

  def apply(id: String) = new Sample(id, SampleClassHelper(), None)

  def apply(id: String, map: Map[String, String]) = new Sample(id, SampleClassHelper(map), None)
}
