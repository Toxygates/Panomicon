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

package t
import t.db.BioObject
import t.db.GenBioObject

import scala.collection.DefaultMap

package object sparql {

  import scala.collection.mutable.{ HashMap, MultiMap, Map, Set }
  import scala.collection.{ Set => CSet, Map => CMap }
  import scala.language.implicitConversions

  type SMMap = CMap[String, CSet[String]]
  type SMPMap = CMap[String, CSet[(String, String)]]
  type MMap[K, T] = CMap[K, CSet[T]]
  type BBMap = MMap[_ <: GenBioObject, _ <: GenBioObject]

  def emptySMMap() = emptyMMap[String, String]()
  def emptySMPMap() = emptyMMap[String, (String, String)]()
  def emptyMMap[T, U]() = makeRich(CMap[T, CSet[U]]())

  def makeMultiMap[T, U](values: Iterable[(T, U)]): RichMMap[T, U] = {
    val r = new HashMap[T, Set[U]] with MultiMap[T, U]
    for ((k, v) <- values) {
      r.addBinding(k, v)
    }
    makeRich(r)
  }

  def toBioMap[T, U](c1: Iterable[T], f: (T) => Iterable[U]): RichMMap[T, U] =
    makeRich(Map() ++ c1.map(x => (x -> f(x).toSet)))

  implicit def makeRich[T, U](data: MMap[T, U]): RichMMap[T, U] = new RichMMap(data)
  class RichMMap[T, U](data: MMap[T, U]) extends DefaultMap[T, CSet[U]] {
    override def get(key: T) = data.get(key)
    override def iterator = data.iterator
    override def foreach[V](f: ((T, CSet[U])) => V): Unit = data.foreach(f)
    override def size = data.size
    def mapMValues[V](f: U => V) = new RichMMap(map(x => (x._1 -> x._2.map(f))))
    def mapKValues[V](f: T => V) = new RichMMap(map(x => (f(x._1) -> x._2)))
    def allValues = flatMap(_._2)

    def union(m2: MMap[T, U]): RichMMap[T, U] = {
      val allKeys = keySet ++ m2.keySet
      makeRich(Map() ++ allKeys.map(k => k -> (getOrElse(k, CSet()) ++ m2.getOrElse(k, CSet()))))
    }

    def combine[V](m2: MMap[U, V]): MMap[T, V] =
      map(x => (x._1 -> x._2.flatMap(u => m2.getOrElse(u, CSet()))))
    def combine[V](lookup: (Iterable[U]) => MMap[U, V]): MMap[T, V] =
      combine(lookup(flatMap(_._2)))

    def reverse: MMap[U, T] = {
      val entries = data.toVector.flatMap(entry => entry._2.map(x => (x, entry._1)))
      makeMultiMap(entries)
    }
  }

  def bracket(url: String) = "<" + url + ">"
  def unbracket(url: String) = url.replace("<", "").replace(">", "")
}
