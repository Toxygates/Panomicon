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

package t.sparql

import scala.collection.JavaConversions.asScalaSet

import t.db._
import t.Factory
import t.model.sample.CoreParameter._
import t.model.sample.Attribute
import t.model.sample.AttributeSet

/**
 * Metadata from a triplestore.
 * @param sampleStore The triplestore to be queried.
 * @param querySet the parameters to be obtained. The default case returns all parameters.
 */
class TriplestoreMetadata(sampleStore: Samples, val attributeSet: AttributeSet,
    querySet: Iterable[Attribute] = Seq())
(implicit sf: SampleFilter) extends Metadata {

  override def samples: Iterable[Sample] = sampleStore.samples(SampleClassFilter())

  override def sampleAttributes(s: Sample): Seq[(Attribute, String)] = {
    sampleStore.parameterQuery(s.identifier, querySet).collect( {
      case (sp, Some(s)) => (sp, s)
    })
  }

  override def attributeValues(attribute: Attribute): Set[String] =
    sampleStore.sampleAttributeQuery(attribute)(sf)().toSet

  override def mapParameter(fact: Factory, key: String, f: String => String) = ???
}

/**
 * Caching triplestore metadata that reads all the data once and stores it.
 */
class CachingTriplestoreMetadata(os: Samples, attributes: AttributeSet,
    querySet: Iterable[Attribute] = Seq())(implicit sf: SampleFilter)
    extends TriplestoreMetadata(os, attributes, querySet) {

  val useQuerySet = (querySet.toSeq :+ SampleId).distinct

  override lazy val sampleIds = rawData.keySet

  lazy val rawData = {
    val raw = os.sampleAttributeQuery(useQuerySet)(sf)()
    Map() ++ raw.map(r => r(SampleId) -> r)
  }

  lazy val data =
    rawData.mapValues(sample => {
      Map() ++ sample.sampleClass.getKeys().toSeq.
          map(key => key ->  sample.get(key).get)
    })

  // all attributes for a sample
  override def sampleAttributes(s: Sample) =
    data.getOrElse(s.sampleId, Map()).toSeq

  // all values for a given parameter
  override def attributeValues(attribute: Attribute): Set[String] =
    data.flatMap(_._2.get(attribute)).toSet

}
