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

package t.server.viewer.intermine;

import scala.collection.JavaConverters._
import org.intermine.pathquery.Constraints
import org.intermine.pathquery.PathQuery
import t.db.DefaultBio
import t.intermine.Query
import t.platform.PlatformRegistry
import t.sparql._
import t.sparql.secondary.Gene

object TargetmineColumns {
  def connector(mines: Intermines,
      platforms: PlatformRegistry) =
        new IntermineConnector(mines.byTitle("TargetMine"), platforms)

  def miRNA(connector: IntermineConnector) =
    new IntermineColumn(connector,
        "Gene.miRNAInteractions.miRNA.secondaryIdentifier",
        Seq("Gene.miRNAInteractions.miRNA.secondaryIdentifier"))
//        "Gene.miRNAInteractions.miRNA.primaryIdentifier",
//        Seq("Gene.miRNAInteractions.miRNA.symbol"))
}

/**
 * An association column that is resolved by querying an Intermine
 * data warehouse.
 * @param idView This view is used to provide identifiers for the resulting objects.
 * @param titleViews These views are concatenated to generate the titles of the
 * resulting objects.
 */
class IntermineColumn(connector: IntermineConnector,
    idView: String, titleViews: Iterable[String]) extends Query(connector) {

  val geneIdView = "Gene.primaryIdentifier"

//  private val ls = connector.getListService(None, None)
//  ls.setAuthentication(token)

  private def makeQuery(): PathQuery = {
    val pq = new PathQuery(model)
    pq.addViews(geneIdView, idView)
    pq.addViews(titleViews.toSeq: _*)
    pq
  }

  private def tryParseInt(ident: String): Option[Int] = {
    try {
      Some(Integer.parseInt(ident))
    } catch {
      case nf: NumberFormatException => None
    }
  }

  /**
   * Resolve the column based on NCBI genes
   */
  def forGenes(genes: Iterable[Gene]): MMap[Gene, DefaultBio] = {
    val useGenes = genes.flatMap(g => tryParseInt(g.identifier)).toSeq.distinct

    val pq = makeQuery
    pq.addConstraint(Constraints.lookup("Gene", useGenes.mkString(","), ""))

    println(s"${queryService.getCount(pq)} results")
    makeMultiMap(
      queryService.getRowListIterator(pq).asScala.toSeq.map(row => {
        val id = row.get(2).toString
        //Note: might retrieve and insert the score of the association as well
        val tooltip = Some(s"$id (miRTarBase via TargetMine) experimental: true")
        Gene(row.get(0).toString) ->
        DefaultBio(row.get(1).toString, row.get(2).toString, tooltip)
      })
      )

//    val list = connector.addEntrezList(ls, () => genes.map(_.identifier).toSeq.distinct,
//        None, false, List())
//    list match {
//      case Some(l) =>
//        println(s"Added list as ${l.getName} size ${l.getSize} status ${l.getStatus}")
//        val pq = makeQuery
//        pq.addConstraint(Constraints.in("Gene", l.getName))
//        for (row <- qs.getRowListIterator(pq)) {
//          println(Vector() ++ row)
//        }
//        println(s"${qs.getCount(pq)} results")
//
//        emptyMMap()
//      case None => throw new IntermineException("Failed to add temporary list")
//    }
  }
}
