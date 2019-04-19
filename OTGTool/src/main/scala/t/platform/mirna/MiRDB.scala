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
      val spl = l.split("\\s+")
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
