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

package t.sparql

import java.util.Date

import t.TriplestoreConfig

/**
 * Datasets group batches in such a way that the user
 * can control visibility.
 * Each batch belongs to exactly one dataset.
 */
object DatasetStore extends RDFClass {
  val defaultPrefix: String = s"$tRoot/dataset"
  val memberRelation = "t:visibleIn"
  val itemClass = "t:dataset"
}

case class Dataset(id: String, timestamp: Date, comment: String, publicComment: String,
                   description: String, numBatches: Int = 0)

class DatasetStore(config: TriplestoreConfig) extends ListManager[Dataset](config) with BatchGrouping {
  import Triplestore._

  def memberRelation = DatasetStore.memberRelation
  def itemClass: String = DatasetStore.itemClass
  def groupClass = DatasetStore.itemClass
  def groupPrefix = DatasetStore.defaultPrefix
  def defaultPrefix = DatasetStore.defaultPrefix

  def descriptions: Map[String, String] = {
    Map() ++ triplestore.mapQuery(s"$tPrefixes\nSELECT ?l ?desc WHERE { ?item a $itemClass; rdfs:label ?l ; " +
      "t:description ?desc } ").map(x => {
      x("l") -> x("desc")
    })
  }

  def numBatches: Map[String, Int] = {
    Map() ++ triplestore.mapQuery(s"$tPrefixes\nSELECT ?l (COUNT(?b) as ?n) { ?b a t:batch; t:visibleIn ?d. " +
        " ?d a t:dataset; rdfs:label ?l } GROUP BY ?l").flatMap(x => {
        x.get("l") match {
          case Some(label) => Some((label -> x("n").toInt))
          case _ => None
        }
    })
  }

  def setDescription(name: String, desc: String) = {
    triplestore.update(s"$tPrefixes delete { <$defaultPrefix/$name> t:description ?desc } " +
      s"where { <$defaultPrefix/$name> t:description ?desc } ")
    triplestore.update(s"$tPrefixes insert data { <$defaultPrefix/$name> t:description " +
      "\"" + desc + "\" } ")
  }

  /**
   * Return only datasets that have batches in the given instance.
   * @param instanceURI
   * @return
   */
  def withBatchesInInstance(instanceURI: String): Seq[String] = {
    triplestore.simpleQuery(s"$tPrefixes\nSELECT DISTINCT ?l WHERE " +
      s"{ ?item a $itemClass; rdfs:label ?l. " +
      s"?b a ${BatchStore.itemClass}; $memberRelation ?item; " +
        s"${BatchStore.memberRelation} <$instanceURI> }")
  }

  private def additionalFilter(instanceUri: Option[String]) =
    instanceUri match {
      case Some(uri) => s"FILTER EXISTS { ?b a ${BatchStore.itemClass}; $memberRelation ?item; " +
        s"${BatchStore.memberRelation} <$uri> }"
      case None => ""
    }

  override def items(instanceUri: Option[String]): Iterable[Dataset] = {

    val instanceFilter = additionalFilter(instanceUri)
    val descriptions = this.descriptions
    val keyAttribs = keyAttributes(instanceFilter)
    val nb = numBatches
    keyAttribs.map(x => {
      Dataset(x.id, x.timestamp, x.comment, x.publicComment, descriptions.getOrElse(x.id, ""), nb.getOrElse(x.id, 0))
    })
  }

  def list(instanceUri: Option[String]): Seq[String] =
    super.list(additionalFilter(instanceUri))

}
