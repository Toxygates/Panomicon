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

package t.viewer.server

import scala.collection.JavaConversions._
import t.common.shared.SampleClass
import scala.language.implicitConversions
import java.util.{ Map => JMap, HashMap => JHMap, Set => JSet, HashSet => JHSet, List => JList }
import scala.collection.{Map => CMap, Set => CSet}
import t.db.{ExprValue => TExprValue}
import t.common.shared.FirstKeyedPair
import sun.font.CMap
import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Sample
import sun.font.CMap

object Conversions {
	implicit def scAsScala(sc: SampleClass): t.sparql.SampleClass =
	  new t.sparql.SampleClass(mapAsScalaMap(sc.getMap))

	implicit def asSpecies(sc: SampleClass): otg.Species.Species =
	  otg.Species.withName(sc.get("organism"))

	implicit def scAsJava(sc: t.db.SampleClassLike): SampleClass =
	  new SampleClass(new java.util.HashMap(mapAsJavaMap(sc.constraints)))

	def asJavaSample(s: t.db.Sample): Sample = {
    val sc = scAsJava(s.sampleClass)
    new Sample(s.sampleId, sc)
  }

	def asScalaSample(s: Sample) = {
	  val sc = scAsScala(s.sampleClass())
	  new t.db.Sample(s.id, t.db.SampleClass(sc.constraints), None)
	}

  implicit def asJava(ev: TExprValue): ExpressionValue = new ExpressionValue(ev.value, ev.call)
  //Loses probe information!
  implicit def asScala(ev: ExpressionValue): TExprValue = TExprValue(ev.getValue, ev.getCall, "")

  //NB this causes the pairs to be considered equal based on the first item (title) only.
  def asJavaPair[T,U](v: (T, U)) = new t.common.shared.FirstKeyedPair(v._1, v._2)

   //Convert from scala coll types to serialization-safe java coll types.
  def convertPairs(m: CMap[String, CSet[(String, String)]]): JHMap[String, JHSet[FirstKeyedPair[String, String]]] = {
    val r = new JHMap[String, JHSet[FirstKeyedPair[String, String]]]
    val mm: CMap[String, CSet[FirstKeyedPair[String, String]]] = m.map(k => (k._1 -> k._2.map(asJavaPair(_))))
    addJMultiMap(r, mm)
    r
  }

   def convert(m: CMap[String, CSet[String]]): JHMap[String, JHSet[String]] = {
    val r = new JHMap[String, JHSet[String]]
    addJMultiMap(r, m)
    r
  }

  def addJMultiMap[K, V](to: JHMap[K, JHSet[V]], from: CMap[K, CSet[V]]) {
    for ((k, v) <- from) {
      if (to.containsKey(k)) {
        to(k).addAll(v)
      } else {
        to.put(k, new JHSet(v))
      }
    }
  }
}
