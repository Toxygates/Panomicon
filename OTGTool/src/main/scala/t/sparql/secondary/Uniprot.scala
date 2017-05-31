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

package t.sparql.secondary

import otg.Species._
import t.sparql.Triplestore
import t.sparql._
import org.openrdf.repository.RepositoryConnection

trait Uniprot extends Triplestore {

  val prefixes = commonPrefixes + """
    PREFIX up:<http://purl.uniprot.org/core/>
    PREFIX prot:<http://purl.uniprot.org/uniprot/>
    PREFIX taxo:<http://purl.uniprot.org/taxonomy/>
"""

  //constrained to hsa, mmus and rno (taxo 9606, 10090 and 10116)
  //NOT supported in the local filtered Uniprot data (not included yet)
  def keggOrthologs(protein: Protein): Vector[Protein] = {
    simpleQuery(prefixes + " SELECT distinct ?p { " +
      "prot:" + protein.identifier + " rdfs:seeAlso ?ko. " +
      """?ko up:database <http://purl.uniprot.org/database/KO>.
    ?p rdfs:seeAlso ?ko.
    ?p up:organism ?org.
    FILTER (?org IN (taxo:9606, taxo:10090, taxo:10116)) """ +
      "?p rdfs:seeAlso ?ko. } ", false, 60000).map(p => Protein.unpackUniprot(unbracket(p)))
  }

  import scala.collection.{ Map => CMap, Set => CSet }

  //  database can be: eggNOG, KO, ...
  //    def orthologsInDB(proteins: Iterable[Protein], species: Species,
  //        database: String = "eggNOG"): MMap[Protein, Protein] = {
  //      val packedProts = proteins.map(p => "prot:" + p.identifier)
  //
  //      val r = multiQuery(prefixes + " SELECT distinct ?p ?orth {" +
  //      packedProts.map(p =>
  //      		"{ " + p + " rdfs:seeAlso ?ogroup. " +
  //      		" ?ogroup up:database <http://purl.uniprot.org/database/" + database + ">. " +
  //      		" ?orth up:organism taxo:" + species.taxon + "; rdfs:seeAlso ?ogroup. } ").mkString(" \n UNION \n ") +
  //      		" }"
  //      )(60000).map(x =>
  //        (Protein.unpackUniprot(unbracket(x(0))) ->
  //      	Protein.unpackUniprot(unbracket(x(1))))
  //      	)
  //      makeMultiMap(r)
  //    }

  def orthologsInDB(proteins: Iterable[Protein], species: Species,
    database: String = "eggNOG"): MMap[Protein, Protein] = {

    val r = mapQuery(prefixes + """ SELECT distinct ?p ?orth {
		    ?p rdfs:seeAlso ?ogroup .
		    ?ogroup up:database <http://purl.uniprot.org/database/""" + database + "> . " +
      "?orth up:organism taxo:" + species.taxon + " ; " +
      "rdfs:seeAlso ?ogroup . " +
      multiFilter("?p", proteins.map(p => bracket(p.packUniprot))) +
      " } ", 60000).map(x =>
      (Protein.unpackUniprot(unbracket(x("p"))) ->
        Protein.unpackUniprot(unbracket(x("orth")))))
    makeMultiMap(r)
  }

  /**
   * Orthologs in the given species for the given proteins.
   */
  def orthologsFor(uniprots: Iterable[Protein], s: Species): MMap[Protein, Protein] = {
    try {
      //      B2RIProClass.connect()
      //first obtain the corresponding orthologs
      val orthologs = if (!uniprots.isEmpty) {
        orthologsInDB(uniprots, s)
      } else { emptyMMap[Protein, Protein]() }
      println(orthologs)
      //      val allUniprots = orthologs.values.flatten
      //      val genes = if (!allUniprots.isEmpty) { B2RIProClass.geneIdsForUniProts(allUniprots) } else { Map[String, CSet[String]]() }
      //      println(genes)
      //      orthologs.map(x => (x._1 -> x._2.flatMap(up => genes.getOrElse(up, Set()))))
      orthologs
    } finally {
      //      B2RIProClass.close()
    }
  }
}

/**
 * The official Uniprot endpoint. Ortholog queries are slow.
 */
class OfficialUniprot extends Triplestore with Uniprot {
  val con = Triplestore.connectSPARQLRepository("http://beta.sparql.uniprot.org/sparql", null)
}
/**
 * A filtered local subset of the uniprot data. Ortholog queries are faster.
 */
class LocalUniprot(val con: RepositoryConnection) extends Triplestore with Uniprot {
  override def keggOrthologs(protein: Protein): Vector[Protein] = {
    throw new Exception("Unsupported operation")
  }
}
