/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
import t.sparql.Triplestore

/**
 * SPARQL queries and filters for pathology data.
 */
object PathologySparql {
  val prefixes = s""" |${t.sparql.Triplestore.tPrefixes}
                      |PREFIX local:<http://127.0.0.1:3333/>""".stripMargin

  def constrainAll(filter: Pathology): String =
    findingFilter(filter: Pathology) + topographyFilter(filter: Pathology) + gradeFilter(filter: Pathology)

  def findingFilter(filter: Pathology) =
    filter.finding.map("?p local:find_id ?f . ?f local:label \"" + _ + "\". ").
      getOrElse("")

  def topographyFilter(filter: Pathology) =
    filter.topography.map("?p local:topo_id ?f . ?f local:label \"" + _ + "\". ").
      getOrElse("")

  def gradeFilter(filter: Pathology) =
    filter.grade.map("?p local:grade_id ?f . ?f local:label \"" + _ + "\". ").
      getOrElse("")

  def pathologyQuery(ts: Triplestore, constraints: String): Vector[Pathology] = {

    val r = ts.mapQuery(s"""$prefixes
      |SELECT DISTINCT ?spontaneous ?grade ?topography ?finding ?image WHERE {
      |  GRAPH ?gr1 { $constraints }
      |  GRAPH ?gr2 { ?x t:pathology ?p . OPTIONAL { ?x t:pathology_digital_image ?image . } }
      |  GRAPH ?gr3 { ?p local:find_id ?f; local:topo_id ?t;
      |    local:grade_id ?g; local:spontaneous_flag ?spontaneous . }
      |  GRAPH ?gr4 { ?f local:label ?finding . }
      |  GRAPH ?gr5 { OPTIONAL { ?g local:label ?grade . } }
      |  GRAPH ?gr6 { OPTIONAL { ?t local:label ?topography . } }
      |}""".stripMargin)

    /*
     * We should remove the need to check for suffixes like 1> etc. (Legacy)
     */
    r.map(x =>
      Pathology(x.get("finding"), x.get("topography"),
        x.get("grade"), x("spontaneous").endsWith("1>"), "", x.getOrElse("image", null)))
  }
}
