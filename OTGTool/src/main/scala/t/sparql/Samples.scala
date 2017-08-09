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

import t.BaseConfig
import t.TriplestoreConfig
import t.db.Sample
import t.db.SampleParameter
import t.sparql.{ Filter => TFilter }
import t.model.sample.Attribute
import scala.collection.JavaConversions._

object Samples extends RDFClass {
  val defaultPrefix = s"$tRoot/sample"
  val itemClass = "t:sample"
}

/**
 * Generates SPARQL fragments to filter samples by
 * instances, batches, and datasets
 */
case class SampleFilter(instanceURI: Option[String] = None,
  batchURI: Option[String] = None,
  datasetURIs: List[String] = List()) {

  def visibilityRel(variable: String) = instanceURI match {
    case Some(u) => s"$variable ${Batches.memberRelation} <$u> ."
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

abstract class Samples(bc: BaseConfig) extends ListManager(bc.triplestore)
  with t.sample.SampleSet {
  import Triplestore._
  import QueryUtils._

  def itemClass: String = Samples.itemClass
  def defaultPrefix = Samples.defaultPrefix

  val standardAttributes = bc.sampleParameters.required.map(_.identifier)
  val hlAttributes = bc.attributes.getHighLevel.map(_.id)
  val tsCon: TriplestoreConfig = bc.triplestore

  val hasRelation = "t:hasSample"
  def hasRelation(batch: String, sample: String): String =
    s"<${Batches.defaultPrefix}/$batch> $hasRelation <$defaultPrefix/$sample>"

  def addSamples(batch: String, samples: Iterable[String]): Unit = {
    triplestore.update(tPrefixes + " " +
      "insert data { " +
      samples.map(s => hasRelation(batch, s)).mkString(". ") +
      samples.map(s => s"$defaultPrefix/$s a $itemClass.").mkString(" ") +
      " }")
  }

  //TODO is this the best way to handle URI/title conversion?
  //Is such conversion needed?
  protected def adjustSample(map: Map[String, String],
                             overrideBatch: Option[String] = None): Map[String, String] = {
    var result = if (map.contains("dataset")) {
      map + ("dataset" -> Datasets.unpackURI(map("dataset")))
    } else {
      map
    }
    overrideBatch match {
      case Some(ob) => result + ("batchGraph" -> ob)
      case _ => result
    }
    result
  }

  protected def graphCon(g: Option[String]) = g.map("<" + _ + ">").getOrElse("?g")

  /**
   * Constructs a sample query that respects a SampleClassFilter and normal SampleFilter
   */
  def sampleQuery(sc: SampleClassFilter)(implicit sf: SampleFilter): Query[Vector[Sample]]

  def samples() = ???

  def samples(sc: SampleClassFilter)(implicit sf: SampleFilter): Seq[Sample] =
    sampleQuery(sc)(sf)()

  def allValuesForSampleAttribute(attribute: String,
    graphURI: Option[String] = None): Iterable[String] = {
    val g = graphCon(graphURI)

    val q = tPrefixes +
      s"SELECT DISTINCT ?q WHERE { GRAPH $g { ?x a t:sample; t:$attribute ?q } }"
    triplestore.simpleQuery(q)
  }

  /**
   * Find the platforms represented in a set of samples
   */
  def platforms(samples: Iterable[String]): Iterable[String] = {
    triplestore.simpleQuery(tPrefixes + " SELECT DISTINCT ?p WHERE { GRAPH ?batchGraph { " +
      "?x a t:sample; rdfs:label ?id; t:platform_id ?p }  " +
      multiFilter("?id", samples.map("\"" + _ + "\"")) +
      " }")
  }

  def samples(sc: SampleClassFilter, fparam: String, fvalues: Iterable[String])(implicit sf: SampleFilter): Seq[Sample] = {
    sampleQuery(sc).constrain(
      multiFilter(s"?$fparam", fvalues.map("\"" + _ + "\"")))()
  }

  def sampleClasses(implicit sf: SampleFilter): Seq[Map[String, String]]

  def parameters(sample: Sample): Seq[(Attribute, String)] =
    parameters(sample, Seq())

  override def parameters(sample: Sample,
    querySet: Iterable[Attribute]): Seq[(Attribute, String)] =
    parameterQuery(sample.sampleId, querySet).collect {
      case (sp, Some(v)) => (sp, v)
    }
  
  /**
   * Can the given predicate ID be queried as a predicate of a sample?
   */
  protected def isPredicateAttribute(attribute: String) = 
    attribute != "batchGraph"
  
  /**
   * In the supplied collection, remove all attributes that are not predicates. 
   */
  protected def predicateAttributes(attributes: Iterable[String]) = 
    attributes.filter(isPredicateAttribute(_))

  /**
   * Get parameter values, if present, for a given sample
   * @param querySet the set of parameters to fetch. If ordered, we preserve the ordering in the result
   */
  def parameterQuery(sample: String,
    querySet: Iterable[Attribute] = Seq()): Seq[(Attribute, Option[String])] = {
    
    val attrs = otg.model.sample.AttributeSet.getDefault
    val queryParams = (if (querySet.isEmpty) {

      //TODO this is temporary during refactoring.
      //future: do not use bc.sampleParameters at all
      bc.sampleParameters.all.map(p =>
        attrs.findOrCreate(p.id(), p.title(),
            if (p.isNumerical()) "numerical" else "text"))
    } else {
      querySet
    }).toSeq.filter(a => isPredicateAttribute(a.id))

    val withIndex = queryParams.zipWithIndex
    val triples = withIndex.map(x => " OPTIONAL { ?x t:" + x._1.id + " ?k" + x._2 + ". } ")
    val query = "SELECT * WHERE { GRAPH ?batchGraph { " +
      "{ { ?x rdfs:label \"" + sample + "\" } UNION" +
      "{ ?x rdfs:label \"" + sample + "\"^^xsd:string } }" +
      triples.mkString + " } } "
    val r = triplestore.mapQuery(tPrefixes + query)
    if (r.isEmpty) {
      List()
    } else {
      val h = r.head
      withIndex.map(x => (x._1, h.get("k" + x._2)))
    }
  }

  /**
   * Get all distinct values for an attribute inside specified SampleFilter
   */
  def sampleAttributeQuery(attribute: String)(implicit sf: SampleFilter): Query[Seq[String]] = {
    if (!isPredicateAttribute(attribute)) {
      throw new Exception("Invalid query")
    }
    
    Query(tPrefixes,
      "SELECT DISTINCT ?q " +
        s"WHERE { GRAPH ?batchGraph { " +
        "?x t:" + attribute + " ?q . ",
      s"} ${sf.standardSampleFilters} } ",
      triplestore.simpleQueryNonQuiet)
  }
  
  /**
   * Get all distinct values for a set of attributes inside specified SampleFilter
   */
  def sampleAttributeQuery(attribute: Seq[String])
    (implicit sf: SampleFilter): Query[Seq[Map[String, String]]] = {
    val pattr = predicateAttributes(attribute)
    
    Query(tPrefixes,
      "SELECT DISTINCT * " +
        " WHERE { GRAPH ?batchGraph { " +
        "?x " +
          pattr.map(x => s"t:$x ?$x").mkString("; "),
      s"} ${sf.standardSampleFilters} } ",
      triplestore.mapQuery(_, 10000))
  }

  def attributeValues(filter: TFilter, attribute: String)(implicit sf: SampleFilter) =
    sampleAttributeQuery(attribute).constrain(filter)()

  def sampleGroups(sf: SampleFilter): Iterable[(String, Iterable[Sample])] = {
    val q = tPrefixes +
      "SELECT DISTINCT ?l ?sid WHERE { " +
      s"?g a ${SampleGroups.itemClass}. " +
      sf.visibilityRel("?g") +
      s"?g ${SampleGroups.memberRelation} ?sid; rdfs:label ?l" +
      "}"

    //Note that ?sid is the rdfs:label of samples

    val mq = triplestore.mapQuery(q)
    val byGroup = mq.groupBy(_("l"))
    val allIds = mq.map(_("sid")).distinct
    val withAttributes = sampleQuery(SampleClassFilter())(sf).constrain(
      "FILTER (?id IN (" + allIds.map('"' + _ + '"').mkString(",") + ")).")()
    val lookup = Map() ++ withAttributes.map(x => (x.identifier -> x))

    for (
      (group, all) <- byGroup;
      samples = all.flatMap(m => lookup.get(m("sid")))
    ) yield (group, samples)
  }
}
