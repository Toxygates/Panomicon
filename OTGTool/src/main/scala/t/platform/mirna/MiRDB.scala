package t.platform.mirna

import scala.io.Source
import java.io.PrintWriter

/**
 * Tool for converting filtered mirDB data into suitable
 * RDF format.
 */
object MiRDBConverter {

  /*
   * Example records:
   * hsa-let-7a-2-3p NM_153690       54.8873
   * hsa-let-7a-2-3p NM_001127715    92.914989543
   */
  def main(args: Array[String]) {
    val lines = Source.fromFile(args(0)).getLines
    //trig format
    val w = new PrintWriter(args(1))
    try {
      val graph = "<http://level-five.jp/t/mapping/mirdb>"
      val label = "miRDB 5.0"
      w.println(s"""|@prefix t:<http://level-five.jp/t/>.
                    |@prefix tp:<http://level-five.jp/t/probe/>.
                    |@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
                    |$graph {
                    |  $graph a t:mirnaSource; t:hasScores "true"; rdfs:label "$label";
                    |  t:suggestedLimit "50"; t:empirical "false". """.stripMargin)
      for (l <- lines) {
        val Array(mirna, refseq, score) = l.split("\\s+")
        w.println(s"""  [] t:mirna tp:$mirna; t:refseqTrn "$refseq"; t:score "$score".""")
      }
      w.println("}")
    } finally {
      w.close()
    }
  }

}
