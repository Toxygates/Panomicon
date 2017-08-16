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
class TriplestoreMetadata(sampleStore: Samples, val attributes: AttributeSet,
    querySet: Iterable[Attribute] = Seq())
(implicit sf: SampleFilter) extends Metadata {

  override def samples: Iterable[Sample] = sampleStore.samples(SampleClassFilter())

  override def parameters(s: Sample): Seq[(Attribute, String)] = {
    sampleStore.parameterQuery(s.identifier, querySet).collect( {
      case (sp, Some(s)) => (sp, s)
    })
  }

  override def parameterValues(identifier: String): Set[String] =
    sampleStore.allValuesForSampleAttribute(identifier).toSet

  override def mapParameter(fact: Factory, key: String, f: String => String) = ???

  //TODO
  override def isControl(s: Sample) = ???
}

/**
 * Caching triplestore metadata that reads all the data once and stores it.
 */
class CachingTriplestoreMetadata(os: Samples, attributes: AttributeSet,
    querySet: Iterable[Attribute] = Seq())(implicit sf: SampleFilter)
    extends TriplestoreMetadata(os, attributes, querySet) {

  val useQuerySet = (querySet.toSeq :+ SampleId).distinct

  lazy val rawData = {
    val raw = os.sampleAttributeQuery(useQuerySet)(sf)()
    Map() ++ raw.map(r => r("sample_id") -> r)
  }

  println(rawData take 10)

  lazy val data = {    
    rawData.map(sample => (sample._1 -> sample._2.flatMap {case (k,v) => 
        Option(attributes.byId(k)).map(a => (a,v)) }))          
  }

  println(data take 10)

  override def parameters(s: Sample) = data.getOrElse(s.sampleId, Map()).toSeq

  override def parameterValues(identifier: String): Set[String] =
    data.map(_._2(attributes.byId(identifier))).toSet
}
