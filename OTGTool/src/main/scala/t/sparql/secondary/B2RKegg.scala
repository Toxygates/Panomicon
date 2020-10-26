/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import t.platform.Species._
import t.sparql._
import t.db._
import org.eclipse.rdf4j.repository.RepositoryConnection
import t.platform.Species

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
  def platformTaxon(plat: String): String =
    Species.forKnownPlatform(plat).map(_.shortCode.toUpperCase).
      getOrElse(plat)

}

class B2RKegg(val conn: RepositoryConnection) extends Triplestore with Store[Pathway] { //RemoteRDF

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
    val genes = simpleQuery(s"$prefixes\n + SELECT DISTINCT ?gene { $q }").map(g => Gene.unpackKegg(g)).toSet
    pw.copy(genes = genes)
  }

  private[sparql] def attributes(pw: Pathway): (String, String) = {
    val (constraint, endFilter) = if (pw.identifier == null) {
      ("dc:title ?title", "FILTER(STR(?title)=\"" + pw.name + "\")")
    } else {
      ("bv:uri " + bracket(pw.identifier), "")
    }
    val q = s""" GRAPH ?pwGraph {
      ?pw $constraint .
      ?g2 kv:pathway ?pw;
      kv:x-ncbigene ?gene. } $endFilter """

    (prefixes, q)
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
   * Obtain all pathways associated with each of a set of genes.
   */
  def forGenes(genes: Iterable[Gene]): MMap[Gene, Pathway] = {
    def convert(uri: String, g: Gene): String = {
      //convert from e.g. http://bio2rdf.org/kegg:map03010 to rno003010
      val taxon = g.keggShortCode.toLowerCase
      taxon + uri.split("map")(1)
    }

    val r = mapQuery(prefixes +
      """SELECT DISTINCT ?g2 ?title ?uri where { graph ?pwGraph {
        ?g2 kv:x-ncbigene ?g; kv:pathway ?pw.
        ?pw bv:uri ?uri; dc:title ?title . """ +
      multiFilter("?g", genes.map(g => bracket(g.packKeggNCBI))) +
      " } } ")

    makeMultiMap(r.map(x => {
      val g = Gene.unpackKegg(x("g2"))
      g -> Pathway(convert(x("uri"), g), x("title"))
    }))
  }

}
