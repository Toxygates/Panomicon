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

package t.platform.mirna

import scala.io.Source
import java.io.PrintWriter
import t.platform._

/**
 * Tool for ingesting filtered mirDB data.
 */
class MiRDBConverter(inputFile: String, dbName: String) {
  def size =  Source.fromFile(inputFile).getLines.size

  /*
   * Example records:
   * hsa-let-7a-2-3p NM_153690       54.8873
   * hsa-let-7a-2-3p NM_001127715    92.914989543
   */
  def lines = Source.fromFile(inputFile).getLines
  val info = new ScoreSourceInfo(dbName)

  def makeTable = {
    val builder = new TargetTableBuilder

    for (l <- lines) {
      val spl = l.split("\t")
      builder.add(MiRNA(spl(0)), RefSeq(spl(1)), spl(2).toDouble,
          info)
    }
    builder.build
  }

  def makeTrig(output: String) {
    import MiRDBConverter._
    val w = new PrintWriter(output)
    try {
      val graph = s"<$mirdbGraph>"
      val label = "miRDB 5.0"
      w.println(s"""|@prefix t:<http://level-five.jp/t/>.
                    |@prefix tp:<http://level-five.jp/t/probe/>.
                    |@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
                    |@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
                    |$graph {
                    |  $graph a t:mirnaSource; t:hasScores "true"; rdfs:label "$label";
                    |  t:suggestedLimit 50.0; t:experimental "false"; t:size $size. """.stripMargin)
      for (l <- lines) {
        val Array(mirna, refseq, score) = l.split("\\s+")
        w.println(s"""  [] t:mirna tp:$mirna; t:refseqTrn "$refseq"; t:score $score.""")
      }
      w.println("}")
    } finally {
      w.close()
    }
  }
}

object MiRDBConverter {
  val mirdbGraph = "http://level-five.jp/t/mapping/mirdb"

  def main(args: Array[String]) {
    new MiRDBConverter(args(0), "MiRDB 5.0").makeTrig(args(1))
  }
}
