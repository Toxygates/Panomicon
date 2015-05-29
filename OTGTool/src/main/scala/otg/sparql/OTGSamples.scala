/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import otg._
import org.openrdf._
import org.openrdf.repository._
import org.openrdf.repository.manager._
import org.openrdf.query._
import org.openrdf.model._
import org.openrdf.rio._
import scala.collection.JavaConversions._
import t.TriplestoreConfig
import t.sparql.Triplestore
import t.sparql.{Filter => TFilter}
import t.sparql.SampleClass
import t.db.Sample
import t.sparql.Samples
import t.sparql.SimpleTriplestore
import t.sparql.Query
import t.BaseConfig
import t.sparql.secondary.GOTerm
import t.sparql.SampleGroups
import t.sparql.Instances
import t.sparql.Datasets
import t.sparql.SampleFilter

class OTGSamples(bc: BaseConfig) extends Samples(bc) {
  
  val prefixes = commonPrefixes + """    	
    PREFIX go:<http://www.geneontology.org/dtds/go.dtd#> 
"""
  
  //TODO case with no attributes won't work
  //TODO consider lifting up
  //TODO query var's are repeated when SampleClass.filterAll is used
  def sampleQuery(implicit sf: SampleFilter): Query[Vector[Sample]] 
  = Query(prefixes,
     "SELECT * WHERE { GRAPH ?batchGraph { ?x a t:sample; " +
      standardAttributes.map(a => s"t:$a ?$a").mkString("; ") + "." +         
      "?x rdfs:label ?id. OPTIONAL { ?x t:control_group ?control_group . } ", 
      s" } ${sf.standardSampleFilters} }", 
      eval = (q => ts.mapQuery(q)(20000).map(x => { 
       val sc = SampleClass(adjustSample(x)) 
       Sample(x("id"), sc, x.get("control_group"))        
      })))

  def sampleClasses(implicit sf: SampleFilter): Seq[Map[String, String]] = {
    //TODO case with no attributes  
    //TODO may be able to lift up to superclass and generalise
    
    val vars = hlAttributes.map(a => s"?$a").mkString(" ")
    val r = ts.mapQuery(prefixes +
      "SELECT DISTINCT " + vars + """ WHERE { 
        graph ?batchGraph { ?x a t:sample; """ +
      hlAttributes.map(a => s"t:$a ?$a").mkString("; ") + ". } " +      
      s"${sf.standardSampleFilters} }")
    r.map(adjustSample(_))
  }
  
  def compounds(filter: TFilter)(implicit sf: SampleFilter) = 
    sampleAttributeQuery("t:compound_name").constrain(filter)()
  
  def pathologyQuery(constraints: String): Vector[Pathology] = {
    val r = ts.mapQuery(prefixes + 
        "SELECT DISTINCT ?spontaneous ?grade ?topography ?finding ?image WHERE {" + 
        constraints + 
      """ ?x local:pathology ?p .
      ?p local:find_id ?f . 
      ?p local:topo_id ?t . 
      ?p local:grade_id ?g . 
      ?p local:spontaneous_flag ?spontaneous . 
      ?f local:label ?finding . 
      OPTIONAL { ?x local:pathology_digital_image ?image . } 
      OPTIONAL { ?t local:label ?topography . }         
      ?g local:label ?grade. } """)
      
    r.map(x => 
      Pathology(x.get("finding"), x.get("topography"), 
          x.get("grade"), x("spontaneous").endsWith("1>"), "", x.getOrElse("image", null))        
      )      
  }
  
  def pathologies(barcode: String): Vector[Pathology] = {
    val r = pathologyQuery("?x rdfs:label \"" + barcode + "\". ")
      r.map(_.copy(barcode = barcode))
  }

  // Returns: (human readable, identifier, value)
  override def annotationQuery(sample: String, 
      querySet: List[String] = Nil): Iterable[(String, String, Option[String])] = {
      val annotations = if (querySet == Nil) { 
        Annotation.keys.toList
      } else {
        Annotation.keys.filter(a => querySet.contains(a._1)).toList
      }
    
    val withIndex = annotations.zipWithIndex
    val triples = withIndex.map(x => " OPTIONAL { ?x t:" + x._1._2 + " ?k" + x._2 + ". } ")
    val query = "SELECT * WHERE { GRAPH ?batchGraph { ?x rdfs:label \"" + sample + "\" . " + 
    		triples.mkString + " } } "  
    val r = ts.mapQuery(prefixes + query)
    if (r.isEmpty) {
      List()
    } else {
      val h = r.head
      withIndex.map(x => (x._1._1, x._1._2, h.get("k" + x._2)))
    }
  }
  
  /**
   * Produces human-readable values
   */
  override def annotations(sample: String, querySet: List[String] = Nil): Annotation = {
    val m = annotationQuery(sample, querySet).map(x => 
      (x._1, x._3.getOrElse("N/A"))).toSeq
    Annotation(m, sample).postReadAdjustment      
  }

}
