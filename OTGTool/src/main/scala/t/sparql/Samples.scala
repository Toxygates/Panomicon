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

import t.BaseConfig
import t.TriplestoreConfig
import t.db.{Sample}
import t.sparql.{ Filter => TFilter }
import t.model.sample.Attribute
import scala.collection.JavaConverters._
import t.model.sample.CoreParameter._
import t.model.SampleClass
import t.model.sample.AttributeSet

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

  def standardSampleFilters = s"$instanceFilter " +
    s"?batchGraph ${Datasets.memberRelation} ?dataset. " +
    s"?dataset a ${Datasets.itemClass}." +
    s"$datasetFilter $batchFilter"
}

abstract class Samples(bc: BaseConfig) extends ListManager(bc.triplestore)
  with t.sample.SampleSet {
  import Triplestore._
  import QueryUtils._

  def itemClass: String = Samples.itemClass
  def defaultPrefix = Samples.defaultPrefix

  val standardAttributes = bc.attributes.getRequired.asScala.toSeq
  val hlAttributes = bc.attributes.getHighLevel.asScala.toSeq
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

  /**
   * Adjust parameters of the sample after loading from the triplestore.
   */
  protected def adjustSample(map: Map[String, String],
                             overrideBatch: Option[String] = None): Map[String, String] = {
    var result = if (map.contains("dataset")) {
      map + ("dataset" -> Datasets.unpackURI(map("dataset")))
    } else {
      map
    }
    result = overrideBatch match {
      case Some(ob) => result + ("batchGraph" -> ob)
      case _ => result
    }
    result
  }

  /**
   * Converts the keys in a Map from String to Attribute using the supplied
   * AttributeSet. Keys not found in the AttributeSet will be ommitted.
   */
  protected def convertMapToAttributes(map: Map[String, String],
      attributeSet: AttributeSet): Map[Attribute, String] = {
    map.map(x => Option(bc.attributes.byId(x._1)) -> x._2)
      .collect { case (Some(attrib), value) => (attrib, value) }
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
      s"SELECT DISTINCT ?q WHERE { GRAPH $g { ?x a $itemClass; t:$attribute ?q } }"
    triplestore.simpleQuery(q)
  }

  /**
   * Find the platforms represented in a set of samples
   */
  def platforms(samples: Iterable[String]): Iterable[String] = {
    triplestore.simpleQuery(tPrefixes + " SELECT DISTINCT ?p WHERE { GRAPH ?batchGraph { " +
      s"?x a $itemClass; rdfs:label ?id; t:platform_id ?p }  " +
      multiFilter("?id", samples.map("\"" + _ + "\"")) +
      " }")
  }

  def samples(sc: SampleClassFilter, fparam: String, fvalues: Iterable[String])(implicit sf: SampleFilter): Seq[Sample] = {
    sampleQuery(sc).constrain(
      multiFilter(s"?$fparam", fvalues.map("\"" + _ + "\"")))()
  }

  def sampleClasses(implicit sf: SampleFilter): Seq[Map[Attribute, String]]

  def sampleAttributes(sample: Sample): Seq[(Attribute, String)] =
    sampleAttributes(sample, Seq())

  override def sampleAttributes(sample: Sample,
    querySet: Iterable[Attribute]): Seq[(Attribute, String)] =
    parameterQuery(sample.sampleId, querySet).collect {
      case (sp, Some(v)) => (sp, v)
    }

  /**
   * Can the given predicate ID be queried as a predicate of a sample?
   */
  protected def isPredicateAttribute(attribute: Attribute): Boolean =
    attribute != Batch

  /**
   * Get parameter values for a set of samples. Values will only be returned
   * for samples that have values for *all* of the parameters requested.
   * @param querySet the set of parameters to fetch. If ordered, we preserve the ordering in the result
   */
  def sampleAttributeValues(samples: Iterable[String],
    queryAttribs: Iterable[Attribute] = Seq()): Map[String, Seq[(Attribute, Option[String])]] = {

    val queryParams = (if (queryAttribs.isEmpty) {
      bc.attributes.getAll.asScala.toSeq
    } else {
      queryAttribs
    }).toSeq.filter(a => isPredicateAttribute(a))
    val withIndex = queryParams.zipWithIndex
    val vars = withIndex.map("?k" + _._2 + " ").mkString
    val triples = withIndex.map(x => " ?x t:" + x._1.id + " ?k" + x._2 + ".  ").mkString
    val sampleIds = samples.map("\"" + _ + "\" ").mkString
    val sampleIdsXsd = samples.map("\"" + _ + "\"^^xsd:string ").mkString

    val queryResult = triplestore.mapQuery(s"""$tPrefixes
      |SELECT ?label $vars WHERE {
      |  GRAPH ?g {
      |    $triples
      |    ?x rdfs:label ?label. VALUES ?label {$sampleIds $sampleIdsXsd}
      |  }
      |}""".stripMargin)

    val groupedResult = queryResult.groupBy(_.get("label"))

    Map() ++ (for {
      idOption <- groupedResult.keys
      sampleId <- idOption
      paramsForId = groupedResult(Some(sampleId)).head
    } yield sampleId -> withIndex.map(x => (x._1, paramsForId.get("k" + x._2))))
  }

  /**
   * Get parameter values, if present, for a given sample
   * @param querySet the set of parameters to fetch. If ordered, we preserve the ordering in the result
   */
  def parameterQuery(sample: DSampleId,
    querySet: Iterable[Attribute] = Seq()): Seq[(Attribute, Option[String])] = {

    //val attrs = otg.model.sample.AttributeSet.getDefault
    val queryParams = (if (querySet.isEmpty) {
      bc.attributes.getAll.asScala.toSeq
    } else {
      querySet
    }).toSeq.filter(a => isPredicateAttribute(a))

    val withIndex = queryParams.zipWithIndex
    val triples = withIndex.map(x => " OPTIONAL { ?x t:" + x._1.id + " ?k" + x._2 + ". } ")
    val query = "SELECT * WHERE { GRAPH ?batchGraph { " +
      "{ { ?x rdfs:label \"" + sample + "\" } UNION" +
      "{ ?x rdfs:label \"" + sample + "\"^^xsd:string } }" +
      triples.mkString + " } } "
    val r = triplestore.mapQuery(tPrefixes + '\n' + query)
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
  def sampleAttributeQuery(attribute: Attribute)(implicit sf: SampleFilter): Query[Seq[String]] = {
    if (!isPredicateAttribute(attribute)) {
      throw new Exception("Invalid query")
    }
    
    Query(tPrefixes,
      "SELECT DISTINCT ?q WHERE { " +
        s"${sf.standardSampleFilters} " + 
        "GRAPH ?batchGraph { " +
        "?x t:" + attribute.id + " ?q . ",
      s"} }",
      triplestore.simpleQueryNonQuiet)
  }

  /**
   * Get all distinct values for a set of attributes inside specified SampleFilter
   */
  def sampleAttributeValueQuery(attributes: Seq[Attribute])
    (implicit sf: SampleFilter): Query[Seq[Map[String, String]]] = {
    val pattr = attributes.filter(isPredicateAttribute)

    Query(tPrefixes,
      s"""|SELECT DISTINCT *
          |  WHERE { GRAPH ?batchGraph {
          |    ?x ${pattr.map(x => s"t:${x.id} ?${x.id}").mkString("; ")}""".stripMargin,
          s"} ${sf.standardSampleFilters} } ",
      triplestore.mapQuery(_, 10000))
  }

  /**
   * For a given set of attributes, count the number of samples for each existing
   * distinct combination of attribute values.
   */
  def sampleCountQuery(attributes: Iterable[Attribute])(implicit sf: SampleFilter):
    Query[Seq[Map[String, String]]] = {
    import otg.model.sample.OTGAttribute._

    val pattr = attributes.filter(isPredicateAttribute).toSeq ++ Seq(Compound, DoseLevel)
    val queryVars = pattr.map(a => s"?${a.id}").mkString(" ")

    //For adjuvant compounds, e.g. ADDA.ID and ADDA.IP, this gives us the name
    //without the administration route suffix
    val cmpPar = Compound.id
    val extraVars = if (pattr.contains(Compound)) {
      s"(SUBSTR(?$cmpPar, 0, strlen(?$cmpPar) - 2) as ?${cmpPar}Edit)"
    } else ""

    Query(tPrefixes,
      s"""|SELECT $queryVars (STR(COUNT(DISTINCT *)) AS ?count) $extraVars
          |  WHERE { GRAPH ?batchGraph {
          |    ?x ${pattr.map(x => s"t:${x.id} ?${x.id}").mkString("; ")}""".stripMargin,
      s"""|  }
          |  ${sf.standardSampleFilters}
          |  FILTER (?dose_level != "Control" && ?compound_name != "Shared_control")
          |}
          |GROUP BY $queryVars""".stripMargin,
          triplestore.mapQuery(_, 10000))
  }

  /**
   * For all samples within a specified sample class, create Sample objects
   * storing the sample's ID as well as all specified attributes. Samples
   * missing any of the specified attributes, however, will not be fetched.
   * Does not support specification of batch graph in the SampleClassFilter.
   */
  def sampleAttributeQuery(attributes: Iterable[Attribute], sampleClassFilter: SampleClassFilter =
    SampleClassFilter())
    (implicit sampleFilter: SampleFilter): Query[Seq[Sample]] = {

    val queryAttributes = (attributes.filter(isPredicateAttribute).toSeq :+ SampleId).distinct
    val filterAttributes = sampleClassFilter.constraints.keys.filter(isPredicateAttribute)
    
    val sampleClassFilterString = filterAttributes
      .map(a => s"t:${a.id}" + " \"" + s"${sampleClassFilter.constraints(a)}" + "\"")
      .mkString("; ")

    Query(tPrefixes,
        s"""SELECT ${ queryAttributes.map('?' + _.id).mkString(" ") }
           |  WHERE {
           |  ${sampleFilter.standardSampleFilters}  
           |  GRAPH ?batchGraph {
           |    ?x ${ (queryAttributes ++ filterAttributes).distinct.map(a => s"t:${a.id} ?${a.id}").mkString("; ") };
           |    $sampleClassFilterString
           |""".stripMargin,
        s"""} 
           |  }""".stripMargin,
        triplestore.mapQuery(_, 20000).map(s => {
          val sampleClass = new SampleClass((convertMapToAttributes(s, bc.attributes)
              ++ sampleClassFilter.constraints).asJava)
          val id = s(SampleId.id)
          Sample(id, sampleClass)
          }
    ))
  }

  def attributeValues(filter: TFilter, attribute: Attribute)(implicit sf: SampleFilter) =
    sampleAttributeQuery(attribute).constrain(filter)()

  def sampleGroups(sf: SampleFilter): Iterable[(String, Iterable[Sample])] = {
    val q = tPrefixes + '\n' +
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
