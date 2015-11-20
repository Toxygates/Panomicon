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

package t.sparql.secondary

import otg.Species._
import org.openrdf.repository.RepositoryConnection
import t.sparql._
import t.db.DefaultBio
import t.db.GenBioObject
import t.db.Store
import t.db.StoredBioObject

/**
 * The identifier is a URI
 */
case class Pathway(val identifier: String,
  override val name: String,
  val genes: Iterable[Gene] = Set()) extends StoredBioObject[Pathway] {

  override def hashCode = name.hashCode

  override def equals(other: Any): Boolean = other match {
    case Pathway(_, oname, _) => oname == name
    case _                    => false
  }
}

object B2RKegg {
  //OTG only. TODO: this is temporary, too hardcoded
  def platformTaxon(plat: String): String = {
    plat match {
      case "Rat230_2"       => "RNO"
      case "Mouse430_2"     => "MMU"
      case "HG-U133_Plus_2" => "HSA"
      case _                => plat
    }
  }
}

class B2RKegg(val con: RepositoryConnection) extends Triplestore with Store[Pathway] { //RemoteRDF

  //  val URL = "http://kegg.bio2rdf.org/sparql" //for remote use

  //"""PREFIX kr:<http://bio2rdf.org/ns/kegg#>
  val prefixes = commonPrefixes + """
    PREFIX kv:<http://bio2rdf.org/kegg_vocabulary:>
    PREFIX bv:<http://bio2rdf.org/bio2rdf_vocabulary:>
    PREFIX dc:<http://purl.org/dc/terms/>
"""

  def genes(pw: Pathway): Iterable[Gene] = withAttributes(pw).genes
  def geneIds(pathway: String): Iterable[String] = genes(Pathway(null, pathway)).map(_.identifier)

  override def withAttributes(pw: Pathway): Pathway = {
    val (prefixes, q) = attributes(pw)
    val genes = simpleQuery(s"$prefixes\n + SELECT DISTINCT ?g { $q }").map(g => Gene.unpackKegg(g)).toSet
    pw.copy(genes = genes)
  }

  //TODO simplify, move functionality into Query
  //track prefixes, selected vars
  private[sparql] def attributes(pw: Pathway): (String, String) = {
    val (constraint, endFilter) = if (pw.identifier == null) {
      ("dc:title ?title", "FILTER(STR(?title)=\"" + pw.name + "\")")
    } else {
      ("bv:uri " + bracket(pw.identifier), "")
    }
    val q = s" GRAPH ?pwGraph { ?pw $constraint . " +
      s"?ko kv:pathway ?pw; kv:gene ?kgene; t:entrez ?gene } $endFilter "

    (prefixes, q)
  }

  def forPattern(pattern: String): Vector[String] = {
    simpleQuery(prefixes +
      """SELECT DISTINCT ?title where { graph ?gr {
        ?pw rdf:type kv:Pathway;
        dc:title ?title .  """ +
      "filter regex(?title, '.*" + pattern + ".*', 'i') " +
      "} } limit 100").toVector
  }

  def forPattern(pattern: String, maxSize: Int): Vector[String] = {
     simpleQuery(prefixes +
      """SELECT DISTINCT ?title where { graph ?gr {
        ?pw rdf:type kv:Pathway;
        dc:title ?title .  """ +
        "filter regex(?title, '.*" + pattern + ".*', 'i') " +
        "} } order by ?title limit " + maxSize).toVector
  }

  /**
   * Obtain all enzymes associated with each of a set of genes.
   * TODO: remove or upgrade
   */
  def enzymes(genes: Iterable[Gene], species: Species): MMap[Gene, DefaultBio] = {
    val r = multiQuery(prefixes +
      """SELECT DISTINCT ?g ?ident ?url where {
    	?pw kv:xReaction/kv:xEnzyme ?en;
           rdf:type kv:Pathway ;
    	   kv:xTaxon <http://bio2rdf.org/kegg_taxon:""" + species.shortCode + "> . " +
      " ?en kv:xGene ?g ; rdfs:label ?ident ; bio2rdf:url ?url . " +
      multiFilter("?g", genes.map(g => bracket(g.packKegg))) + " . }" //TODO
      ).map(x => Gene.unpackKegg(unbracket(x(2))) -> DefaultBio(x(0), x(1)))
    makeMultiMap(r)
  }

  /**
   * Obtain all pathways associated with each of a set of genes.
   */
  def forGenes(genes: Iterable[Gene]): MMap[Gene, Pathway] = {
    def convert(uri: String, g: Gene): String = {
      //convert from e.g. http://bio2rdf.org/kegg:map03010
      val taxon = g.keggShortCode.toLowerCase
      "http://www.genome.jp/dbget-bin/www_bget?path:" + taxon + uri.split("map")(1)
    }

    val r = mapQuery(prefixes +
      """SELECT DISTINCT ?g ?title ?uri where { graph ?pwGraph {
        ?ko kv:gene ?g; kv:pathway ?pw.
        ?pw bv:uri ?uri; dc:title ?title . """ +
      multiFilter("?g", genes.map(g => bracket(g.packKegg))) +
      " } } ")

    makeMultiMap(r.map(x => {
      val g = Gene.unpackKegg(x("g"))
      g -> Pathway(convert(x("uri"), g), x("title"))
    }))
  }

  //For Tritigate. TODO: Unify with the above.
  def forGenesOSA(genes: Iterable[GenBioObject]): MMap[DefaultBio, Pathway] = {
    //convert from e.g. http://bio2rdf.org/kegg:map03010
    def convert(uri: String) =
      "http://www.genome.jp/dbget-bin/www_bget?path:osa" + uri.split("map")(1)

    val r = mapQuery(prefixes +
      """SELECT DISTINCT ?osagene ?title ?uri where { graph ?gr {
        ?g t:symbol ?osagene.
        ?ko kv:gene ?g; kv:pathway ?pw.
        ?pw bv:uri ?uri; dc:title ?title . """ +
      multiFilter("?osagene", genes.map(g => "\"" + g.identifier + "\"")) +
      " } } ")
    makeMultiMap(r.map(x =>
      DefaultBio(x("osagene"), x("osagene")) ->
        Pathway(convert(x("uri")), x("title"))))
  }

  def OSAGeneSyms(pathway: String): Iterable[String] = {
    val q = prefixes +
      """SELECT DISTINCT ?osagene WHERE { GRAPH ?gr {
    ?g t:symbol ?osagene.
    ?ko kv:gene ?g; kv:pathway ?pw.
    ?pw dc:title ?titl. } """ +
      "FILTER (str(?titl) = \"" + pathway + "\") }"
    simpleQuery(q)
  }

  //For enrichment. May be slow.
  def osaGeneToPathwayFullMap: MMap[DefaultBio, Pathway] =
    forGenesOSA(List())

}
