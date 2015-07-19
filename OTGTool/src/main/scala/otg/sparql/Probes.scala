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
import org.openrdf.repository.RepositoryConnection
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

  val prefixes = commonPrefixes

  def proteins(pr: Probe): Iterable[Protein] = withAttributes(List(pr)).flatMap(_.proteins)
  def genes(pr: Probe): Iterable[Gene] = withAttributes(List(pr)).flatMap(_.genes)

  override def allGeneIds(): MMap[Probe, Gene] = {
    val query = prefixes + "SELECT DISTINCT ?p ?x WHERE { GRAPH ?g { " +
     "?p a t:probe ;" +
     " t:entrez ?x . } } "
     makeMultiMap(ts.mapQuery(query).map(x => (Probe.unpack(x("p")), Gene(x("x")))))
  }

  //TODO share query-forming code with superclass instead of totally overriding it
  override def withAttributes(probes: Iterable[Probe]): Iterable[Probe] = {
      def obtain(m: Map[String, String], key: String) = m.getOrElse(key, "")

	  val q = prefixes +
	  """
	     SELECT * WHERE { GRAPH ?g {
	  	 ?pr rdfs:label ?l .
	     optional { ?pr t:symbol ?symbol. }
	     optional { ?pr t:swissprot ?prot. }
	     optional { ?pr t:entrez ?gene. }
	     optional { ?pr t:title ?title. }
	     ?pr a t:probe . """ +
	  multiFilter("?pr", probes.map(p => bracket(p.pack))) + " } ?g rdfs:label ?plat} "
	  val r = ts.mapQuery(q)(20000)

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
    val query = prefixes + "SELECT DISTINCT ?s WHERE { GRAPH ?g { " +
    s"?p a $itemClass . " +
    "?p t:symbol ?s . " +
    "?s " + prefixStringMatch(title + "*") +
    "} " +
    platform.map(x => "?g rdfs:label \"" + x + "\"").getOrElse("") +
    " } limit 10"
    ts.mapQuery(query).map(x => Probe(x("s")))
  }

  /**
   * Based on a set of gene symbols, return the corresponding probes.
   * probes.
   * TODO: constrain by platform
   */
  def forGeneSyms(symbols: Iterable[Gene], precise: Boolean): MMap[Gene, Probe] = {
      val query = prefixes +
        "SELECT DISTINCT ?p ?gene WHERE { GRAPH ?g { " +
        "?p a t:probe . " +
        "?p t:symbol ?gene . " +
        caseInsensitiveMultiFilter("?gene",
          if (precise) {
            symbols.map("\"^" + _.symbol + "$\"")
          } else {
            symbols.map("\"" + _.symbol + "\"")
          }) +
          " } ?g rdfs:label ?plat }"

        val r = ts.mapQuery(query).map(
          x => (symbols.find(s =>
            s.symbol.toLowerCase == x("gene").toLowerCase)
            .getOrElse(null) -> Probe.unpack(x("p"))))
      makeMultiMap(r.filter(_._1 != null))
  }

  def mfGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
    goTerms("?probe t:gomf ?got . ", probes)
  }

  def ccGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
    goTerms("?probe t:gocc ?got . ", probes)
  }

  def bpGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
   goTerms("?probe t:gobp ?got . ", probes)
  }

  override protected def quickProbeResolution(rs: GradualProbeResolver, precise: Boolean): Unit = {
    //NB Gene(s, symbol=s) is a hack because the first argument is expected to be an entrez id.
    //However we have to put something non-null there to allow the gene objects to have proper
    //hash codes and equality -- think about this.
    if (rs.unresolved.size > 0) {
      val geneSyms = forGeneSyms(
          rs.unresolved.map(s => Gene(s, symbol = s)), precise
      ).allValues
      rs.resolve(geneSyms.map(_.identifier))
    }
  }

  override protected def slowProbeResolution(rs: GradualProbeResolver, precise: Boolean): Unit = {
    val ups = forUniprots(rs.unresolved.map(p => Protein("uniprot:" + p.toUpperCase))).map(_.identifier)
    rs.resolve(ups)
  }

  /**
   * Find GO terms matching the given string.
   * Note that we use go:synonym as well as go:name here for a maximally generous match.
   */
  override def goTerms(pattern: String): Iterable[GOTerm] = {
	  val query = prefixes +
	  "SELECT DISTINCT ?got ?gotn WHERE { GRAPH ?g { " +
	  "{ ?got go:synonym ?gotn . " +
	    "?gotn " + infixStringMatch(pattern) +
	  //"FILTER regex(?gotn, \".*" + pattern + ".*\", \"i\")" +
	  "} UNION " +
	  "{ ?got go:name ?gotn . " +
	    "?gotn " + infixStringMatch(pattern) +
	  //"FILTER regex(?gotn, \".*" + pattern + ".*\", \"i\")" +
	  "} FILTER STRSTARTS(STR(?got), \"http://bio2rdf.org\") " +
	  "} } limit 1000 "
	  ts.mapQuery(query).map(x => GOTerm(x("got"), x("gotn")))
  }

}
