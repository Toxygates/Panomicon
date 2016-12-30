package otg.sparql

import otg.Pathology

// Methods to generate SPARQL constraints corresponding to the parameters
// in this filter. However, we are not currently filtering by pathology anywhere.
class PathologySparql(filter: Pathology) {
  def constrainAll: String = findingFilter + topographyFilter + gradeFilter

  def findingFilter = filter.finding.map("?p local:find_id ?f . ?f local:label \"" + _ + "\". ").
    getOrElse("")

  def topographyFilter = filter.topography.map("?p local:topo_id ?f . ?f local:label \"" + _ + "\". ").
    getOrElse("")

  def gradeFilter = filter.grade.map("?p local:grade_id ?f . ?f local:label \"" + _ + "\". ").
    getOrElse("")
}
