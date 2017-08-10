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

import scala.collection.{ Map => CMap }

import scala.collection.JavaConversions._
import t.model.sample.Attribute

trait SampleClassLike {
  def constraints: CMap[String, String]

  @deprecated("Query by Attribute instead.", "Aug 2017")
  def apply(key: String): String = constraints(key)

  @deprecated("Query by Attribute instead.", "Aug 2017")
  def get(key: String): Option[String] = constraints.get(key)

  def apply(key: Attribute): String = apply(key.id)

  def get(key: Attribute): Option[String] = get(key.id)

  def ++(other: SampleClassLike) =
    new t.model.SampleClass(constraints ++ other.constraints)
}
