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

/**
 * A set of string values that can quickly and reversibly be
 * converted to another type T. Useful for database encodings.
 *
 * TODO: Do we need this trait? Will it do to use a standard collection Map?
 */
trait LookupMap[T] {
  /**
   * Keys that are actually used
   */
  def keys: Set[T]

  def tokens: Set[String]

  def pack(item: String): T
  def unpack(item: T): String
  def tryUnpack(item: T): Option[String]

  def isToken(t: String): Boolean = tokens.contains(t)
}

trait CachedLookupMap[T] extends LookupMap[T] {
  def data: Map[String, T]

  protected[this] val map = data
  protected[this] val revMap = Map[T, String]() ++ map.map(_.swap)

  def keys = revMap.keySet
  def tokens: Set[String] = map.keySet
  def pack(item: String): T = map.get(item).getOrElse(
    throw new LookupFailedException(s"Lookup failed for $item"))
  def unpack(item: T): String = revMap(item)
  def tryUnpack(item: T) = revMap.get(item)
}

abstract class MemoryLookupMap[T](val data: Map[String, T])
  extends CachedLookupMap[T]

class LookupFailedException(reason: String) extends Exception(reason)

/**
 * A probe encoding.
 */
trait ProbeMap extends LookupMap[Int]

trait SampleMap extends LookupMap[Int]
