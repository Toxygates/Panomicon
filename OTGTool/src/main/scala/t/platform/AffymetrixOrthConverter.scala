package t.platform

import scala.io.Source

case class AffyOrthologRecord(title: String, probes: Set[String])

object AffymetrixOrthConverter {

  /**
   * Arguments: input file (Affymetrix format, e.g. Rat230_2.na34.ortholog.csv)
   */
  def main(args: Array[String]): Unit = {
    if (args.size < 1) {
      throw new Exception("Please specify the input file name (.csv)")
    }
    val input = args(0)
    val output = args(0).replace(".csv", "_t.tsv")

    val data =
      Source.fromFile(input).getLines.toVector.drop(1).map(processLine)    
      
    val bySourceAndTitle = data.groupBy(x => (x(0), x(4)))
    val recs = bySourceAndTitle.map {
      case (k, v) => AffyOrthologRecord(k._2, v.map(_(2)).toSet + k._1)
    }
    
    val ps = new java.io.PrintStream(new java.io.FileOutputStream(output))
    Console.withOut(ps) {
      for (r <- recs) {
        println(s"\042${r.title}\042\t${r.probes.map(_.toLowerCase).mkString(",")}")
      }
    }   
  }
  
  def processLine(l: String): Seq[String] = l.split(",").map(ll => ll.replace("\"", ""))
}