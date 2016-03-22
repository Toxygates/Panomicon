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

package t.sparql

import t.BaseConfig
import t.TriplestoreConfig
import t.db.Sample
import t.db.SampleParameter
import t.sparql.{ Filter => TFilter }

object Samples extends RDFClass {
  val defaultPrefix = s"$tRoot/sample"
  val itemClass = "t:sample"
}

case class SampleFilter(instanceURI: Option[String] = None,
  batchURI: Option[String] = None,
  datasetURIs: List[String] = List()) {

  def visibilityRel(v: String) = instanceURI match {
    case Some(u) => s"$v ${Batches.memberRelation} <$u> ."
    case None    => ""
  }

  def instanceFilter: String = visibilityRel("?batchGraph")
  def datasetFilter: String = {
    if (datasetURIs.isEmpty)
      ""
    else
      " FILTER(?dataset IN (" +
        datasetURIs.map(x => s"<$x>").mkString(",") +
        ") )."
  }

  def batchFilter: String = batchURI match {
    case None     => ""
    case Some(bu) => s" FILTER(?batchGraph = <$bu>)."
  }

  def standardSampleFilters = s"\n$instanceFilter " +
    s"?batchGraph ${Datasets.memberRelation} ?dataset. " +
    s"?dataset a ${Datasets.itemClass}." +
    s"$datasetFilter $batchFilter\n"
}

abstract class Samples(bc: BaseConfig) extends ListManager(bc.triplestore) {
  import Triplestore._
  import QueryUtils._

  def itemClass: String = Samples.itemClass
  def defaultPrefix = Samples.defaultPrefix

  val standardAttributes = bc.sampleParameters.required.map(_.identifier)
  val hlAttributes = bc.sampleParameters.highLevel.map(_.identifier)
  val tsCon: TriplestoreConfig = bc.triplestore

  val hasRelation = "t:hasSample"
  def hasRelation(batch: String, sample: String): String =
    s"<${Batches.defaultPrefix}/$batch> $hasRelation <$defaultPrefix/$sample>"

  def addSamples(batch: String, samples: Iterable[String]): Unit = {
    ts.update(tPrefixes + " " +
      "insert data { " +
      samples.map(s => hasRelation(batch, s)).mkString(". ") +
      samples.map(s => s"$defaultPrefix/$s a $itemClass.").mkString(" ") +
      " }")
  }

  //TODO is this the best way to handle URI/title conversion?
  //Is such conversion needed?
  protected def adjustSample(m: Map[String, String]): Map[String, String] = {
    if (m.contains("dataset")) {
      m + ("dataset" -> Datasets.unpackURI(m("dataset")))
    } else {
      m
    }
  }

  protected def graphCon(g: Option[String]) = g.map("<" + _ + ">").getOrElse("?g")

  /**
   * The sample query must query for ?batchGraph and ?dataset.
   */
  def sampleQuery(sc: SampleClass)(implicit sf: SampleFilter): Query[Vector[Sample]]

  def samples(sc: SampleClass)(implicit sf: SampleFilter): Seq[Sample] =
    sampleQuery(sc)(sf)()

  def allValuesForSampleAttribute(attribute: String,
    graphURI: Option[String] = None): Iterable[String] = {
    val g = graphCon(graphURI)

    val q = tPrefixes +
      s"SELECT DISTINCT ?q WHERE { GRAPH $g { ?x a t:sample; t:$attribute ?q } }"
    ts.simpleQuery(q)
  }

  def platforms(samples: Iterable[String]): Iterable[String] = {
    ts.simpleQuery(tPrefixes + " SELECT distinct ?p WHERE { GRAPH ?batchGraph { " +
      "?x a t:sample; rdfs:label ?id; t:platform_id ?p }  " +
      multiFilter("?id", samples.map("\"" + _ + "\"")) +
      " }")
  }

  def samples(sc: SampleClass, fparam: String, fvalues: Iterable[String])(implicit sf: SampleFilter): Seq[Sample] = {
    sampleQuery(sc).constrain(
      multiFilter(s"?$fparam", fvalues.map("\"" + _ + "\"")))()
  }

  def sampleClasses(implicit sf: SampleFilter): Seq[Map[String, String]]

  def parameterQuery(sample: String,
    querySet: Iterable[SampleParameter] = Set()): Iterable[(SampleParameter, Option[String])] = {
    val allParams = bc.sampleParameters.all
    val queryParams = if (querySet.isEmpty) {
      allParams
    } else {
      allParams.filter(querySet.toSet.contains(_))
    }

    val withIndex = queryParams.zipWithIndex
    val triples = withIndex.map(x => " OPTIONAL { ?x t:" + x._1.identifier + " ?k" + x._2 + ". } ")
    val query = "SELECT * WHERE { GRAPH ?batchGraph { " +
      "{ { ?x rdfs:label \"" + sample + "\" } UNION" +
      "{ ?x rdfs:label \"" + sample + "\"^^xsd:string } }" +
      triples.mkString + " } } "
    val r = ts.mapQuery(tPrefixes + query)
    if (r.isEmpty) {
      List()
    } else {
      val h = r.head
      withIndex.map(x => (x._1, h.get("k" + x._2)))
    }
  }

   def sampleAttributeQuery(attribute: String)(implicit sf: SampleFilter): Query[Seq[String]] = {
    Query(tPrefixes,
      "SELECT DISTINCT ?q " +
        s"WHERE { GRAPH ?batchGraph { " +
        "?x " + attribute + " ?q . ",
      s"} ${sf.standardSampleFilters} } ",
      ts.simpleQueryNonQuiet)
  }

  def sampleAttributeQuery(attribute: Seq[String])
    (implicit sf: SampleFilter): Query[Seq[Map[String, String]]] = {

    Query(tPrefixes,
      "SELECT DISTINCT " + attribute.map("?" + _).mkString(" ") +
        " WHERE { GRAPH ?batchGraph { " +
        "?x " +
          attribute.map(x => s"t:$x ?$x").mkString("; "),
      s"} ${sf.standardSampleFilters} } ",
      ts.mapQuery)
  }

  def attributeValues(filter: TFilter, attribute: String)(implicit sf: SampleFilter) =
    sampleAttributeQuery("t:" + attribute).constrain(filter)()

  def sampleGroups(sf: SampleFilter): Iterable[(String, Iterable[Sample])] = {
    val q = tPrefixes +
      "SELECT DISTINCT ?l ?sid WHERE { " +
      s"?g a ${SampleGroups.itemClass}. " +
      sf.visibilityRel("?g") +
      s"?g ${SampleGroups.memberRelation} ?sid; rdfs:label ?l" +
      "}"

    //Note that ?sid is the rdfs:label of samples

    val mq = ts.mapQuery(q)
    val byGroup = mq.groupBy(_("l"))
    val allIds = mq.map(_("sid")).distinct
    val withAttributes = sampleQuery(SampleClass())(sf).constrain(
      "FILTER (?id IN (" + allIds.map('"' + _ + '"').mkString(",") + ")).")()
    val lookup = Map() ++ withAttributes.map(x => (x.identifier -> x))

    for (
      (group, all) <- byGroup;
      samples = all.flatMap(m => lookup.get(m("sid")))
    ) yield (group, samples)
  }
}
