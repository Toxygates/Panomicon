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

package otg

/**
 * A representation of a pathology, as well as a way of filtering by pathologies.
 */
case class Pathology(finding: Option[String], topography: Option[String], grade: Option[String],
  spontaneous: Boolean, barcode: String = null, digitalViewerLink: String = null) {

  // The methods below generate SPARQL constraints corresponding to the parameters
  // in this filter. However, we are not currently filtering by pathology anywhere.
  def constrainAll: String = findingFilter + topographyFilter + gradeFilter

  def findingFilter = finding.map("?p local:find_id ?f . ?f local:label \"" + _ + "\". ").
    getOrElse("")

  def topographyFilter = topography.map("?p local:topo_id ?f . ?f local:label \"" + _ + "\". ").
    getOrElse("")

  def gradeFilter = grade.map("?p local:grade_id ?f . ?f local:label \"" + _ + "\". ").
    getOrElse("")
}
