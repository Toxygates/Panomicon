/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.intermine

import t.platform.Species._
import org.intermine.pathquery.PathQuery
import org.intermine.pathquery.Constraints
import scala.collection.JavaConverters._
import t.sparql.secondary.Gene

/**
 * Given two species, obtain a set of orthologous proteins between them
 * from an Intermine instance.
 */
class OrthologProteins(
  connector: Connector,
  s1: Species, s2: Species) extends Query(connector) {

  def makeQuery(): PathQuery = {
    val pq = new PathQuery(model)
    pq.addViews("Gene.primaryIdentifier", "Gene.proteins.orthologProteins.genes.primaryIdentifier")
    pq.addConstraint(Constraints.eq("Gene.organism.shortName", s1.shortName))
    pq.addConstraint(Constraints.eq("Gene.proteins.orthologProteins.genes.organism.shortName",
      s2.shortName))

    println(s"Intermine query: ${pq.toXml()}")
    pq
  }

  /**
   * In the results, the first gene will be from species s1, and the second from species s2.
   */
  def results: Iterable[(Gene, Gene)] = {
    queryService.getRowListIterator(makeQuery).asScala.toSeq.map(row => {
      ((Gene(row.get(0).toString), Gene(row.get(1).toString)))
    })
  }
}

object OrthologProteins {
  def main(args: Array[String]) {
    val conn = new Connector("targetmine", "http://targetmine.mizuguchilab.org/targetmine/service")
    val op = new OrthologProteins(conn, Rat, Human)
    val res = op.results
    println(res.size + " results")
    println(res take 10)
  }
}
