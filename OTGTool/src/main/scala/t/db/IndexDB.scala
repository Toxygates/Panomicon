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

/**
 * An IndexDB maps human-readable string identifiers (URIs or otherwise) to
 * integer ID:s that may be used for further lookup in other databases.
 * In addition to its default key space, it also stores named enums.
 * Enum values cannot currently be removed.
 */
trait IndexDB {

  /**
   * Retrieve the entire map encoded by this IndexDB.
   */
  def fullMap: Map[String, Int]

  /**
   * Retrieve all keys.
   */
  def keys: Iterable[String] = fullMap.keySet

  /**
   * Retrieve all values.
   */
  def values: Iterable[Int] = fullMap.values

  /**
   * Retrieve a mapping.
   */
  def get(key: String): Option[Int]

  /**
   * Retrieve a value in an enum.
   */
  def get(enum: String, key: String): Option[Int]

  /**
   * Retrieve a full enum mapping.
   */
  def enumMap(name: String): Map[String, Int]

  def enumMaps(keys: Iterable[String]): Map[String, Map[String, Int]] =
    Map() ++ keys.map(k => (k -> enumMap(k)))
}

/**
 * A writable IndexDB.
 */
trait IndexDBWriter extends IndexDB {
  /**
   * Store a mapping.
   * Returns the newly generated value.
   */
  def put(key: String): Int

  /**
   * Store a mapping in an enum.
   * Returns the newly generated value.
   */
  def put(enum: String, key: String): Int

  /**
   * Remove a mapping.
   */
  def remove(key: String): Unit

  /**
   * Find a mapping for the given key, or create it if it didn't exist.
   */
  def findOrCreate(key: String): Int = get(key).getOrElse(put(key))

  /**
   * Find a mapping in an enum, or create it if it didn't exist.
   */
  def findOrCreate(enum: String, key: String) =
    get(enum, key).getOrElse(put(enum, key))

  /**
   * Remove values from the default mapping.
   */
  def remove(keys: Iterable[String]): Unit = {
    for (k <- keys) {
      remove(k)
    }
  }
}

object SampleIndex {
  def fromRaw(data: Map[String, Int]) = new SampleIndex(data.map(x => x._1 -> x._2))
  def fromDb(db: IndexDB) = fromRaw(db.fullMap)
}

class SampleIndex(val data: Map[SampleId, Int]) extends CachedIntLookupMap[SampleId]

class ProbeIndex(val data: Map[ProbeId, Int]) extends CachedIntLookupMap[ProbeId] {
  def this(db: IndexDB) = this(db.fullMap)
}
