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

import scala.collection.JavaConversions._
import otg.Species._
import scala.annotation.tailrec
import otg.Context
import t.TriplestoreConfig
import t.sparql.QueryUtils
import t.sparql.Triplestore
import t.db.ProbeMap
import t.db.DefaultBio
import t.db.Store
import t.sparql._
import t.sparql.secondary.Protein
import t.sparql.secondary.Gene
import t.sparql.secondary.GOTerm
import t.platform.OrthologMapping
import t.platform.Probe
import t.sparql.secondary.B2RKegg

// TODO: quite a bit of code from here should be lifted up
class Probes(config: TriplestoreConfig) extends t.sparql.Probes(config) with Store[Probe] {
  import Probes._

  val prefixes = s"""$commonPrefixes 
    |PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>""".stripMargin

  def proteins(pr: Probe): Iterable[Protein] = withAttributes(List(pr)).flatMap(_.proteins)
  def genes(pr: Probe): Iterable[Gene] = withAttributes(List(pr)).flatMap(_.genes)

  override def allGeneIds(): MMap[Probe, Gene] = {
    val query = s"""$prefixes
      |SELECT DISTINCT ?p ?x WHERE { 
      |  GRAPH ?g { 
      |    ?p a t:probe ; t:entrez ?x . 
      |  }
      |}""".stripMargin
     makeMultiMap(ts.mapQuery(query).map(x => (Probe.unpack(x("p")), Gene(x("x")))))
  }

  //TODO share query-forming code with superclass instead of totally overriding it
  override def withAttributes(probes: Iterable[Probe]): Iterable[Probe] = {
      def obtain(m: Map[String, String], key: String) = m.getOrElse(key, "")

	  val q = s"""$prefixes	  
	    |SELECT * WHERE { 
	    |  GRAPH ?g {
	  	|    ?pr rdfs:label ?l; a t:probe.
	    |    OPTIONAL { ?pr t:symbol ?symbol. }
	    |    OPTIONAL { ?pr t:swissprot ?prot. }
	    |    OPTIONAL { ?pr t:entrez ?gene. }
	    |    OPTIONAL { ?pr t:title ?title. } 
	    |    ${multiFilter("?pr", probes.map(p => bracket(p.pack)))}
      |  } 
      |  ?g rdfs:label ?plat.
      |} """.stripMargin
	  val r = ts.mapQuery(q, 20000)

	  r.groupBy(_("pr")).map(_._2).map(g => {
	    val p = Probe(g(0)("l"))

	    	p.copy(
	    	   proteins = g.map(p => Protein(obtain(p, "prot"))).toSet,
	    	   genes = g.map(p =>
             Gene(obtain(p, "gene"),
                 keggShortCode=B2RKegg.platformTaxon(obtain(g.head, "plat")))
           ).toSet,
	    	   symbols = g.map(p => Gene(obtain(p, "symbol"),
	    		   symbol = obtain(p, "symbol"))).toSet,
	    	   titles = g.map(obtain(_, "title")).toSet, //NB not used
	    	   name = obtain(g.head, "title"),
	    	   platform = obtain(g.head, "plat"))
	      })
  }

  //TODO the platform constraint will only work on Owlim, not Fuseki (with RDF1.1 strings)
  override def probesForPartialSymbol(platform: Option[String], title: String): Vector[Probe] = {
    val query = s"""$prefixes
      |SELECT DISTINCT ?s WHERE { 
      |  GRAPH ?g {
      |    ?p a $itemClass; t:symbol ?s.
      |    ?s + ${prefixStringMatch(title + "*")}
      |  }     
      |  ${platform.map(x => "?g rdfs:label \"" + x + "\"").getOrElse("")}
      |} LIMIT 10""".stripMargin
    ts.mapQuery(query).map(x => Probe(x("s")))
  }

  /**
   * Based on a set of gene symbols, return the corresponding probes.
   * probes.
   * TODO: constrain by platform
   */
  def forGeneSyms(symbols: Iterable[String], precise: Boolean): MMap[String, Probe] = {
    val query = s"""$prefixes
      |SELECT DISTINCT ?p ?gene WHERE { 
      |  GRAPH ?g { 
      |    ?p a t:probe ; t:symbol ?gene . 
      |    ${caseInsensitiveMultiFilter("?gene",
             if (precise) symbols.map("\"^" + _ + "$\"") else symbols.map("\"" + _ + "\"")
           )} 
      |  } 
      |  ?g rdfs:label ?plat }""".stripMargin

    val r = ts.mapQuery(query).map(
      x => (symbols.find(s =>
        s.toLowerCase == x("gene").toLowerCase)
        .getOrElse(null) -> Probe.unpack(x("p"))))
    makeMultiMap(r.filter(_._1 != null))
  }

  def mfGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = 
    goTerms("?probe t:gomf ?got . ", probes)
  
  def ccGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = 
    goTerms("?probe t:gocc ?got . ", probes)
  
  def bpGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = 
   goTerms("?probe t:gobp ?got . ", probes)
  

  def simpleRelationQuery(probes: Iterable[Probe], relation: String): MMap[Probe, DefaultBio] = {
    val query = s"""$prefixes    
      |SELECT DISTINCT ?probe ?result WHERE {
      |  GRAPH ?g { 
      |    ?probe $relation ?result. 
      |  }
      |  ${multiFilter("?probe", probes.map(p => bracket(p.pack)))}
      |}""".stripMargin
    simpleMapQuery(probes, query)
  }

  def refseqTrnLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.RefseqTranscript.key)

  def refseqProtLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.RefseqProtein.key)

  def ensemblLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.Ensembl.key)

  def ecLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.EC.key)

  def unigeneLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.Unigene.key)

  override protected def quickProbeResolution(rs: GradualProbeResolver, precise: Boolean): Unit = {
    if (rs.unresolved.size > 0) {
      val geneSyms = forGeneSyms(rs.unresolved, precise).allValues
      rs.resolve(geneSyms.map(_.identifier))
    }
  }

  override protected def slowProbeResolution(rs: GradualProbeResolver, precise: Boolean): Unit = {
    val ups = forUniprots(rs.unresolved.map(p => Protein(p.toUpperCase))).map(_.identifier)
    rs.resolve(ups)
  }
}
