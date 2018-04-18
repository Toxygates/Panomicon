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
 * A set of string values that can quickly and reversibly be
 * converted to another type T. Useful for database encodings.
 * Unlike a standard Map, efficient bidirectional lookups are supported.
 */
trait LookupMap[T] {
  
  protected def baseMap: Map[T, String]
  
  /**
   * Keys that are actually used
   */
  lazy val keys: Set[T] = baseMap.keySet

  lazy val tokens: Set[String] = baseMap.values.toSet

  def pack(item: String): T 

  def unpack(item: T): String = baseMap(item)
  def tryUnpack(item: T): Option[String] = baseMap.get(item)

  def isToken(t: String): Boolean = tokens.contains(t)
}

trait CachedLookupMap[T] extends LookupMap[T] {
  def data: Map[String, T]
  
  protected val baseMap = Map[T, String]() ++ data.map(_.swap)

  def pack(item: String): T = data.get(item).getOrElse(
    throw new LookupFailedException(s"Lookup failed for $item"))  
}

trait CachedIntLookupMap extends CachedLookupMap[Int] {
  protected val revLookup = Array.tabulate(keys.max + 1)(x => baseMap.get(x))

  override def tryUnpack(x: Int) = revLookup(x)
  override def unpack(x: Int) =  revLookup(x).get
}

class LookupFailedException(reason: String) extends Exception(reason)

