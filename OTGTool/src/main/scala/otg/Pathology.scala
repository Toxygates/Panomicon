package otg


/**
 * A representation of a pathology, as well as a way of filtering by pathologies.
 */
case class Pathology(finding: Option[String], topography: Option[String], grade: Option[String], 
    spontaneous: Boolean, barcode: String = null, digitalViewerLink: String = null) {
  
  // The methods below generate SPARQL constraints corresponding to the parameters
  // in this filter. However, we are not currently filtering by pathology anywhere.
  def constrainAll: String = findingFilter + topographyFilter + gradeFilter   
  
  def findingFilter = finding.map( "?p local:find_id ?f . ?f local:label \"" + _ + "\". " ).
  	getOrElse("")
  	
  def topographyFilter = topography.map( "?p local:topo_id ?f . ?f local:label \"" + _ + "\". " ).
  	getOrElse("")
  	
  def gradeFilter = grade.map( "?p local:grade_id ?f . ?f local:label \"" + _ + "\". ").
  	getOrElse("")
}
