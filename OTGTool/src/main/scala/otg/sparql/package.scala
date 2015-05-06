package otg

import t.sparql.Triplestore
import t.sparql.QueryUtils

//TODO code duplication with t.sparql.secondary
package object sparql extends t.sparql.QueryUtils {
  import Triplestore._

  val commonPrefixes = tPrefixes + """       
    PREFIX local:<http://127.0.0.1:3333/>      
    PREFIX luc: <http://www.ontotext.com/owlim/lucene#>
    PREFIX bio2rdf:<http://bio2rdf.org/ns/bio2rdf#>    
"""
  
  def infixStringMatch(q: String) = " luc:myIndex \"*" + q + "*\". "
  def prefixStringMatch(q: String) = " luc:myIndex \"" + q + "*\". "
  
}
