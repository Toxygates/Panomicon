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
import t.sparql._

class B2RIProClass extends Triplestore {

  /*
   * Note: server unavailable as of Oct 2018
   */
  val conn = Triplestore.connectSPARQLRepository("http://iproclass.bio2rdf.org/sparql")

  val prefixes = """
    PREFIX ipc:<http://bio2rdf.org/iproclass_vocabulary:>
"""

  //    import scala.collection.{ Map => CMap, Set => CSet }
  def geneIdsFor(uniprots: Iterable[Protein]): MMap[Protein, Gene] = {
    val r = multiQuery(prefixes + "SELECT DISTINCT ?up ?gi WHERE { " +
      "?up ipc:x-geneid ?gi. " +
      multiFilter("?up", uniprots.map(p => bracket(p.packB2R))) +
      " } ").map(x => (Protein.unpackB2R(x(0)) -> Gene(unpackGeneid(x(1)))))
    makeMultiMap(r)
  }

  def main(args: Array[String]) {
    try {
      val r = geneIdsFor(List(Protein("Q197F8")))
      println(r)
      println(r.size)
    } finally {
      close()
    }
  }
}
