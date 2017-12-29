package t.viewer.server.network

import t.viewer.shared.network._
import java.io.PrintWriter
import scala.collection.JavaConversions._
import t.util.SafeMath

sealed trait NetworkStyle
case object IDOnly extends NetworkStyle
case object SymbolOnly extends NetworkStyle
case object IDAndSymbol extends NetworkStyle

/**
 * Serializes interaction networks and writes them to files.
 */
class Serializer(network: Network, style: NetworkStyle = IDAndSymbol) {
  def writeTo(file: String, format: Format) {
    format match {
      case Format.DOT => writeDOT(file)
      case Format.Custom => writeCustom(file)
      case Format.SIF => writeSIF(file)
      case _ => throw new Exception("Unsupported format")
    }
  }

  def writeCustom(file: String) {
    val w = new PrintWriter(file)
    try {
      w.println("[nodes]")
      for (n <- network.nodes) {
        w.println(s""" ${n.id} ${n.`type`} ${n.weight} """)
      }

      w.println("[edges]")
      for (i <- network.interactions) {
        w.println(s""" ${i.from.id} ${i.to.id} ${i.label()} ${i.weight()} """)
      }
    } finally {
      w.close()
    }
  }

  /*
   * Reference: http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#sif-format
   */
  def writeSIF(file: String) {
    val w = new PrintWriter(file)
    try {
      for (i <- network.interactions) {
        w.println(s"""${nodeLabel(i.from)}\tpp\t${nodeLabel(i.to)}""")
      }
    } finally{
      w.close
    }
  }
  
  /*
   * Reference: http://www.graphviz.org/pdf/dotguide.pdf
   */
  def writeDOT(file: String) {
    val w = new PrintWriter(file)
    try {
      w.println(s"""|digraph "${network.title}" {
        |  layout=twopi;
        |  nodesep=2;
        |  ranksep=1;""".stripMargin)

      for (n <- network.nodes) {
        w.println(s"""  "${n.id}" [label="${nodeLabel(n)}", ${attributes(n)}]; """)
      }

      for (i <- network.interactions) {
        w.println(s"""  "${i.from.id}" -> "${i.to.id}"; """)
      }

      w.println("}")
    } finally {
      w.close()
    }
  }
  
  def nodeLabel(n: Node) = style match {
    case IDOnly => n.id()
    case SymbolOnly => n.symbol()
    case IDAndSymbol =>
      if (n.symbol != null && n.symbol != "")
        s"${n.symbol} [${n.id}]"
      else
        n.id
  }

  val maxWeight = SafeMath.safeMax(network.nodes.map(_.weight))
  val minWeight = SafeMath.safeMin(network.nodes.map(_.weight))

  def color(n: Node) = {
    if (java.lang.Double.isNaN(n.weight()) ||
        java.lang.Double.isInfinite(n.weight)) {
      "white"
    } else {
      val blue = (127 * (n.weight() - minWeight) / (maxWeight - minWeight)).toInt
      "#%02x%02x%02x".format(0, 128 + blue, 128 + blue)
    }
  }

  def attributes(n: Node) = {
    n.`type` match {
      case "miRNA" => s"""color=blue, fillcolor="${color(n)}" """
      case "mRNA" => s"""color=green, fillcolor="${color(n)}" """
      case _ => ""
    }
  }
}
