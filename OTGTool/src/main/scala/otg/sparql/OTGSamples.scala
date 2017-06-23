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

package otg.sparql

import otg.Pathology
import t.BaseConfig
import t.db.Sample
import t.sparql.{ Filter => TFilter }
import t.sparql.Query
import t.sparql.SampleClass
import t.sparql.SampleFilter
import t.sparql.Samples
import t.db.SampleParameters._

class OTGSamples(bc: BaseConfig) extends Samples(bc) {

  import t.sparql.scToSparql

  val prefixes = commonPrefixes + """
    |PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>""".stripMargin

  //TODO case with no attributes won't work
  //TODO consider lifting up
  def sampleQuery(filter: SampleClass)(implicit sf: SampleFilter): Query[Vector[Sample]] = {
    val filterString = if(filter.constraints.isEmpty) "" else
        s"""|  FILTER(
        |    ${standardAttributes.map(a => filter.get(a).map(f =>
              s"?$a = " + "\"" + f + "\"")).flatten.mkString(" && ")}
        |  )""".stripMargin

    val batchFilter = filter.get("batchGraph")
    val batchFilterQ = batchFilter.map("<" + _ + ">").getOrElse("?batchGraph")

    Query(prefixes,
      s"""SELECT * WHERE {
        |  GRAPH $batchFilterQ {
        |    ?x a t:sample; rdfs:label ?id;
        |    ${standardAttributes.map(a => s"t:$a ?$a").mkString("; ")} .
        |""".stripMargin,

      s"""|} ${sf.standardSampleFilters}
        |  $filterString
        |  }""".stripMargin,

      eval = ts.mapQuery(_, 20000).map(x => {
        val sc = SampleClass(adjustSample(x, batchFilter)) ++ filter
        Sample(x("id"), sc, Some(x(ControlGroup.id)))
       })
     )
  }

  def sampleClasses(implicit sf: SampleFilter): Seq[Map[String, String]] = {
    //TODO case with no attributes
    //TODO may be able to lift up to superclass and generalise

    val vars = hlAttributes.map(a => s"?$a").mkString(" ")
    val r = ts.mapQuery(s"""$prefixes
       |SELECT DISTINCT $vars WHERE {
       |  GRAPH ?batchGraph {
       |    ?x a t:sample;
       |    ${hlAttributes.map(a => s"t:$a ?$a").mkString("; ")} .
       |  }
       |  ${sf.standardSampleFilters}
       |}""".stripMargin)
    r.map(adjustSample(_))
  }

  def compounds(filter: TFilter)(implicit sf: SampleFilter) =
    sampleAttributeQuery("compound_name").constrain(filter)()

  def pathologyQuery(constraints: String): Vector[Pathology] = {
    val r = ts.mapQuery(s"""$prefixes
      |SELECT DISTINCT ?spontaneous ?grade ?topography ?finding ?image WHERE {
      |  $constraints
      |  ?x t:pathology ?p .
      |  ?p local:find_id ?f; local:topo_id ?t; local:grade_id ?g;
      |  local:spontaneous_flag ?spontaneous .
      |  ?f local:label ?finding .
      |  OPTIONAL { ?x t:pathology_digital_image ?image . }
      |  OPTIONAL { ?t local:label ?topography . }
      |  ?g local:label ?grade.
      |}""".stripMargin)

      //TODO clean up the 1> etc
    r.map(x =>
      Pathology(x.get("finding"), x.get("topography"),
        x.get("grade"), x("spontaneous").endsWith("1>"), "", x.getOrElse("image", null)))
  }

  def pathologies(barcode: String): Vector[Pathology] = {
    val r = pathologyQuery("?x rdfs:label \"" + barcode + "\". ")
    r.map(_.copy(barcode = barcode))
  }
}
