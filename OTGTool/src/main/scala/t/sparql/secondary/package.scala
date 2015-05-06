package t.sparql

/**
 * This package contains SPARQL APIs for annotations to
 * probes or samples, etc. The data may be stored locally or remotely.
 */
package object secondary extends QueryUtils {
   import Triplestore._
  
  //TODO: local is due for retirement
  //luc should be reconsidered (in the case of not using OWLIM as a triplestore)
  val commonPrefixes = tPrefixes + """       
    PREFIX local:<http://127.0.0.1:3333/>      
    PREFIX luc: <http://www.ontotext.com/owlim/lucene#>
    PREFIX bio2rdf:<http://bio2rdf.org/ns/bio2rdf#>    
"""

  def stringMatch(q: String) = " luc:myIndex \"*" + q + "*\". "
  
  def unpackGeneid(geneid: String) = geneid.split("geneid:")(1) 
}