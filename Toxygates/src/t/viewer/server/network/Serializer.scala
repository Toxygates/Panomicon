package t.viewer.server.network

import t.viewer.shared.network._
import java.io.PrintWriter
import scala.collection.JavaConversions._

/**
 * Serializes interactrion networks and writes them to files.
 */
class Serializer(network: Network) {
  def writeTo(file: String, format: Format) {
    format match {
      case _: Format.DOT.type => writeDOT(file)
      case _ => throw new Exception("Unsupported format")
    }
  }
  
  /*
   * Reference: http://www.graphviz.org/pdf/dotguide.pdf
   */
  def writeDOT(file: String) {
    val w = new PrintWriter(file)
    w.println(s"""|digraph "${network.title}" {
        |  layout=twopi;
        |  nodesep=2;
        |  ranksep=1;""".stripMargin)
    
    for (n <- network.nodes) {
      w.println(s"""  "${n.id}" [label="${n.id}", ${attributes(n)}]; """)
    }
    
    for (i <- network.interactions) {
      w.println(s"""  "${i.from.id}" -> "${i.to.id}"; """)
    }
    
    w.println("}")
    w.close()
  }
  
  def attributes(n: Node) = {    
    n.`type` match {
      case "miRNA" => "color=blue"
      case "mRNA" => "color=green"
      case _ => ""
    }
  }
}