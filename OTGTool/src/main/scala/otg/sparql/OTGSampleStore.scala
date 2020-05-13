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

package otg.sparql

import otg.Pathology
import t.BaseConfig
import t.db.Sample
import t.sparql._
import t.sparql.{ Filter => TFilter }
import t.model.sample.CoreParameter._
import t.model.sample.OTGAttribute._
import t.model.sample.Attribute

class OTGSampleStore(bc: BaseConfig) extends SampleStore(bc) {

  import t.sparql.scToSparql

  val prefixes = s"$commonPrefixes PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>"

  override def list(): Seq[String] = {
    triplestore.simpleQuery(
        s"""$commonPrefixes
           |SELECT ?l WHERE {
           |  GRAPH ?g { ?x a $itemClass ; rdfs:label ?l } }""".stripMargin)
  }

  def sampleQuery(filter: SampleClassFilter)(implicit sf: SampleFilter): Query[Vector[Sample]] = {
    val standardPred = standardAttributes.filter(isPredicateAttribute)

    val filterString = if(filter.constraints.isEmpty) "" else
        s"""|
            |  FILTER(
            |    ${standardPred.map(attribute => filter.get(attribute).map(value =>
                  s"?${attribute.id} = " + "\"" + value + "\"")).flatten.mkString(" && ")}
            |  )""".stripMargin

    val batchFilter = filter.get(Batch)
    val batchFilterQ = batchFilter.map("<" + _ + ">").getOrElse("?batchGraph")

    Query(prefixes,
      s"""SELECT * WHERE {
        |  GRAPH $batchFilterQ {
        |    ?x a $itemClass; rdfs:label ?id;
        |    ${standardPred.map(a => s"t:${a.id} ?${a.id}").mkString("; ")} .""".stripMargin,

      s"""|} ${sf.standardSampleFilters} $filterString
        |  }""".stripMargin,

      eval = triplestore.mapQuery(_, 20000).map(x => {
        val attributeValues = convertMapToAttributes(adjustSample(x, batchFilter), bc.attributes)
        val sampleId = x("id")
        Sample(sampleId, SampleClassFilter(attributeValues) ++ filter)
       })
     )
  }

  def sampleClasses(implicit sf: SampleFilter): Seq[Map[Attribute, String]] = {
    val hlPred = hlAttributes.filter(isPredicateAttribute)

    val vars = hlPred.map(a => s"?${a.id}").mkString(" ")
    val r = triplestore.mapQuery(s"""$prefixes
       |SELECT DISTINCT $vars WHERE {
       |  GRAPH ?batchGraph {
       |    ?x a $itemClass;
       |    ${hlPred.map(a => s"t:${a.id} ?${a.id}").mkString("; ")} .
       |  }
       |  ${sf.standardSampleFilters}
       |}""".stripMargin)
    r.map(s => convertMapToAttributes(adjustSample(s), bc.attributes))
  }

  def compounds(filter: TFilter)(implicit sf: SampleFilter) =
    sampleAttributeQuery(Compound).constrain(filter)()

  def pathologyQuery(constraints: String): Vector[Pathology] =
    PathologySparql.pathologyQuery(triplestore, constraints)

  def pathologies(query: String): Vector[Pathology] = {
    val r = pathologyQuery("?x rdfs:label \"" + query + "\". ")
    r.map(_.copy(sampleId = query))
  }
}
