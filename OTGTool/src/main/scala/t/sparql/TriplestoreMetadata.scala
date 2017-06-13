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

import t.db.Sample
import t.db.SampleParameter
import t.db.Metadata
import t.db.ParameterSet
import t.Factory

/**
 * Metadata from a triplestore.
 * The graph to be queried can be influenced by setting
 * os.batchURI.
 * @param querySet the parameters to be obtained. The default case returns all parameters.
 */
class TriplestoreMetadata(os: Samples, val parameters: ParameterSet,
    querySet: Iterable[SampleParameter] = Seq())
(implicit sf: SampleFilter) extends Metadata {

  override def samples: Iterable[Sample] = os.samples(SampleClass())

  override def parameters(s: Sample): Iterable[(SampleParameter, String)] = {
    os.parameterQuery(s.identifier, querySet).collect( {
      case (sp, Some(s)) => (sp, s)
    })
  }

  override def parameterValues(identifier: String): Set[String] =
    os.allValuesForSampleAttribute(identifier).toSet

  override def mapParameter(fact: Factory, key: String, f: String => String) = ???

  //TODO
  override def isControl(s: Sample) = ???
}

/**
 * Caching triplestore metadata that reads all the data once and stores it.
 */
class CachingTriplestoreMetadata(os: Samples, parameters: ParameterSet,
    querySet: Iterable[SampleParameter] = Seq())(implicit sf: SampleFilter)
    extends TriplestoreMetadata(os, parameters, querySet) {

  val useQuerySet = (querySet.map(_.identifier).toSeq :+ "sample_id").distinct

  lazy val rawData = {
    val raw = os.sampleAttributeQuery(useQuerySet)(sf)()
    Map() ++ raw.map(r => r("sample_id") -> r)
  }

  println(rawData take 10)

  lazy val data =
    rawData.map(r => (r._1 -> r._2.map { case (k,v) => parameters.byId(k) -> v }))

  println(data take 10)

  override def parameters(s: Sample) = data(s.sampleId)

  override def parameterValues(identifier: String): Set[String] =
    data.map(_._2(parameters.byId(identifier))).toSet
}
