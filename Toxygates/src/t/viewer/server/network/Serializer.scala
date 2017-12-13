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
  
  def writeDOT(file: String) {
    val w = new PrintWriter(file)
    w.println(s"graph ${network.title} {")
    
    for (n <- network.nodes) {
      w.println(s"${n.id} [label=${n.id}, shape=${nodeShape(n)}]")
    }
    
    for (i <- network.interactions) {
      w.println(s"${i.from.id} -> ${i.to.id}")
    }
    
    w.println("}")
    w.close()
  }
  
  def nodeShape(n: Node) = {
    n.`type` match {
      case "mRNA" => "box"
      case _ => "circle"
    }
  }
}