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

package t.platform

import java.io._

import otg.sparql.OTGProbeStore
import t.intermine.{Connector, OrthologProteins}
import t.platform.Species._
import t.sparql.secondary.Gene

import scala.collection.mutable.{HashMap => MHMap, Set => MSet}
import scala.io._

/**
 * Convert SSearch similarity files to TTL format, by using
 * already inserted platform information.
 */
class SSOrthTTL(probes: OTGProbeStore, output: String) {
  import t.util.DoThenClose._

  val probeToGene = probes.allGeneIds()
  val geneToProbe = probeToGene.reverse

  def generateFromIntermine(
    conn:    Connector,
    species: Iterable[(Species, Species)]) {
    val allPairs = species.flatMap(s => new OrthologProteins(conn, s._1, s._2).results)
    generate(allPairs)
  }

  def generateFromFiles(inputFiles: Iterable[String]) {
    val allPairs = inputFiles.flatMap(readPairs)
    generate(allPairs)
  }

  def generate(orthologs: Iterable[(Gene, Gene)]): Unit = {
    val all = new MHMap[Probe, MSet[Probe]]

    for (pair <- orthologs) {
      val ps1 = geneToProbe.get(pair._1)
      val ps2 = geneToProbe.get(pair._2)

      /**
       * This builds the transitive closure of all the
       * ortholog relations
       */
      if (ps1.isDefined && ps2.isDefined) {
        val nw = ps1.get ++ ps2.get
        val existing = nw.filter(all.contains)
        if (existing.nonEmpty) {
          //Join the existing sets together
          val newSet = MSet() ++ nw ++ existing.toSeq.flatMap(all(_))

          for (n <- newSet) {
            all += n -> newSet
          }
        } else {
          val nset = MSet() ++ nw
          all ++= nset.toSeq.map(x => (x -> nset))
        }
      }
    }

    doThenClose(new BufferedWriter(new FileWriter(output)))(fw => {
      fw.write("@prefix t:<http://level-five.jp/t/>. ")
      fw.newLine()
      val pre = t.sparql.ProbeStore.defaultPrefix
      val rel = "t:hasOrtholog"
      var seen = Set[Probe]()
      for ((k, vs) <- all; if (!seen.contains(k))) {
        fw.write(s"[] $rel ")
        fw.write(vs.toSeq.map(v => s"<$pre/${v.identifier}>").mkString(", "))
        fw.write(".")
        fw.newLine()
        seen ++= vs
      }
    })
  }

  /**
   * Read pairs of ENTREZ ids
   */
  def readPairs(in: String): Iterable[(Gene, Gene)] = {
    doThenClose(Source.fromFile(in))(s => {
      s.getLines.toVector.flatMap(l => {
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
    })
  }
}
