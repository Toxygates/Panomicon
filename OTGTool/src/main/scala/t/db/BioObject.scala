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

/**
 * A BioObject is some biological entity that can be uniquely identified
 * by a string. It can also have a name, which by default is the same
 * as the identifier.
 * This trait needs to be implemented by Java classes, so it should
 * have no implementations.
 */
trait BioObject {
  def identifier: String
  def name: String
}

/*
 * Convenience implementations, but this can't be extended by Java classes
 */
trait GenBioObject extends BioObject {
  override def name: String = identifier
  override def hashCode = identifier.hashCode
}

/**
 * The default implementation of a BioObject.
 */
case class DefaultBio(identifier: String, override val name: String = "") extends GenBioObject

trait StoredBioObject[T <: StoredBioObject[T]] extends GenBioObject {
  this: T =>
  def getAttributes(implicit store: Store[T]) = store.withAttributes(List(this)).head
}

/**
 * A Store is a way of looking up additional information about some type of BioObject.
 */
trait Store[T <: StoredBioObject[T]] {

  /**
   * For the given BioObjects of type T, assuming that only the identifier is available,
   * look up all available information and return new copies with all information filled in.
   */
  def withAttributes(objs: Iterable[T]): Iterable[T] = objs

  /**
   * Look up all available information for a single bioObject (where only the identifier
   * needs to be available)
   */
  def withAttributes(obj: T): T = withAttributes(List(obj)).head
}
