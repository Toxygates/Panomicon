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

package otg.platform

import java.io._

import scala.collection.mutable.{ HashMap => MHMap }
import scala.collection.mutable.{ HashMap => MHMap }
import scala.collection.mutable.{ Set => MSet }
import scala.collection.mutable.{ Set => MSet }
import scala.io._

import otg.sparql.Probes
import t.platform.Probe
import t.sparql.secondary.Gene

/**
 * Convert SSearch similarity files to TTL format, by using
 * already inserted platform information.
 */
class SSOrthTTL(probes: Probes,
  inputs: Iterable[String], output: String) {

  val probeToGene = probes.allGeneIds()
  val geneToProbe = probeToGene.reverse

  def generate(): Unit = {
    val all = new MHMap[Probe, MSet[Probe]]
    var allList = List[MSet[Probe]]()

    for (i <- inputs; p <- readPairs(i)) {
      val ps1 = geneToProbe.get(p._1)
      val ps2 = geneToProbe.get(p._2)

      /**
       * This builds the transitive closure of all the
       * ortholog relations
       */
      if (ps1 != None && ps2 != None) {
        val nw = ps1.get ++ ps2.get
        val existing = nw.find(all.contains(_))
        existing match {
          case Some(e) => all(e) ++= nw
          case None => {
            val nset = MSet() ++ nw
            all ++= nset.toSeq.map(x => (x -> nset))
            allList ::= nset
          }
        }
      }
    }

    var fw: BufferedWriter = null
    try {
      fw = new BufferedWriter(new FileWriter(output))
      fw.write("@prefix t:<http://level-five.jp/t/>. ")
      fw.newLine()
      val pre = t.sparql.Probes.defaultPrefix
      val rel = "t:hasOrtholog"
      for (vs <- allList) {
        fw.write(s"[] $rel ")
        fw.write(vs.toList.map(v => s"<$pre/${v.identifier}>").mkString(", "))
        fw.write(".")
        fw.newLine()
      }

    } finally {
      if (fw != null) fw.close()
    }
  }

  /**
   * Read pairs of ENTREZ ids
   */
  def readPairs(in: String): Iterable[(Gene, Gene)] = {
    Source.fromFile(in).getLines.toVector.flatMap(l => {
      val gs = l.split("\t")
      if (gs.length != 2) {
        None
      } else if (gs(0) != "-" && gs(1) != "-") {
        val i1 = Integer.parseInt(gs(0))
        val i2 = Integer.parseInt(gs(1))
        //standard sort order
        if (i1 < i2) {
          Some((Gene(i1), Gene(i2)))
        } else {
          Some((Gene(i2), Gene(i1)))
        }
      } else {
        None
      }
    })
  }
}
