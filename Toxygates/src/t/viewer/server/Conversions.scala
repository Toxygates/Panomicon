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

import java.util.{ HashMap => JHMap }
import java.util.{ HashSet => JHSet }

import scala.collection.JavaConversions._
import scala.collection.{ Map => CMap }
import scala.collection.{ Set => CSet }
import scala.language.implicitConversions

import t.common.shared.FirstKeyedPair
import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Sample
import t.db.{ExprValue => TExprValue}
import t.platform.Species
import otg.model.sample.OTGAttribute

object Conversions {
	implicit def asSpecies(sc: t.model.SampleClass): Species.Species =
	  Species.withName(sc.get(OTGAttribute.Organism))

	def asJavaSample(s: t.db.Sample): Sample =
    new Sample(s.sampleId, s.sampleClass)

	def asScalaSample(s: Sample) =
	  new t.db.Sample(s.id, s.sampleClass)

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
