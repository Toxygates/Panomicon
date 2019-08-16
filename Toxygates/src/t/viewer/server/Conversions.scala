/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import scala.collection.JavaConverters._
import t.common.server.GWTUtils._
import scala.collection.{ Map => CMap }
import scala.collection.{ Set => CSet }
import scala.language.implicitConversions

import t.common.shared.FirstKeyedPair
import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Sample
import t.db.{ExprValue => TExprValue}
import t.platform.Species
import otg.model.sample.OTGAttribute
import t.viewer.shared.AssociationValue
import t.common.shared.GroupUtils
import t.common.shared.sample.Group
import java.util.NoSuchElementException
import t.db.BioObject

object Conversions {
	implicit def asSpecies(sc: t.model.SampleClass): Option[Species.Species] =
	  try {
	    Some(Species.withName(sc.get(OTGAttribute.Organism)))
	  } catch {
	    case nse: NoSuchElementException => None
	  }

	  //Note: it might become necessary to rebuild the sampleClass here if the map type
	  //is not serializable
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
	def convertAssociations(m: CMap[_ <: BioObject, CSet[_ <: BioObject]]):
	  JHMap[String, JHSet[AssociationValue]] = {
	  val r = new JHMap[String, JHSet[AssociationValue]]
	    val mm: CMap[String, CSet[AssociationValue]] =
	      m.map(k =>
	        (k._1.identifier -> k._2.map(x =>
	          new AssociationValue(x.name, x.identifier, x.additionalInfo.getOrElse(null))))
	          )
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
        to.get(k).addAll(v.asGWT)
      } else {
        to.put(k, new JHSet(v.asGWT))
      }
    }
  }

  def asJDouble(x: Double): java.lang.Double =
    new java.lang.Double(x)

  def asJDouble(x: Option[Double]): java.lang.Double =
    x.map(new java.lang.Double(_)).getOrElse(null)

  def groupSpecies(g: Group) =
    Species.withName(GroupUtils.groupAttribute(g, OTGAttribute.Organism))

}
