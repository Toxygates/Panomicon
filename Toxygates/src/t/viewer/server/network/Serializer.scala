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

package t.viewer.server.network

import t.viewer.shared.network._
import java.io.PrintWriter
import scala.collection.JavaConverters._
import t.util.SafeMath

sealed trait NetworkStyle
case object IDOnly extends NetworkStyle
case object SymbolOnly extends NetworkStyle
case object IDAndSymbol extends NetworkStyle

/**
 * Serializes interaction networks and writes them to files.
 */
class Serializer(network: Network, messengerWeightColumn: String, microWeightColumn: String,
    style: NetworkStyle = IDAndSymbol) {
  def writeTo(file: String, format: Format) {
    format match {
      case Format.DOT => writeDOT(file)
      case Format.Custom => writeCustom(file)
      case Format.SIF => writeSIF(file)
      case _ => throw new Exception("Unsupported format")
    }
  }

  def nodeWeight(node: Node): Double = {
    if (node.`type` == Network.mrnaType) {
      node.weights().get(messengerWeightColumn);
    } else {
      node.weights().get(microWeightColumn);
    }
  }

  /**
   * Homebrew format used by us.
   */
  def writeCustom(file: String) {
    val w = new PrintWriter(file)
    try {
      w.println("[nodes]")
      for (n <- network.nodes.asScala) {
        w.println(s""" ${n.id} ${n.`type`} ${nodeWeight(n)} """)
      }

      w.println("[edges]")
      for (i <- network.interactions.asScala) {
        w.println(s""" ${i.from.id} ${i.to.id} ${i.label()} ${i.weight()} """)
      }
    } finally {
      w.close()
    }
  }

  /**
   * SIF format used by e.g. Cytoscape.
   * Reference: http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#sif-format
   */
  def writeSIF(file: String) {
    val w = new PrintWriter(file)
    try {
      for (i <- network.interactions.asScala) {
        w.println(s"""${nodeLabel(i.from)}\tpp\t${nodeLabel(i.to)}""")
      }
    } finally{
      w.close
    }
  }

  /**
   * Dot format used by GraphViz.
   * Reference: http://www.graphviz.org/pdf/dotguide.pdf
   */
  def writeDOT(file: String) {
    val w = new PrintWriter(file)
    try {
      w.println(s"""|digraph "${network.title}" {
        |  layout=twopi;
        |  nodesep=2;
        |  ranksep=1;""".stripMargin)

      for (n <- network.nodes.asScala) {
        w.println(s"""  "${n.id}" [label="${nodeLabel(n)}", ${DOTattributes(n)}]; """)
      }

      for (i <- network.interactions.asScala) {
        w.println(s"""  "${i.from.id}" -> "${i.to.id}"; """)
      }

      w.println("}")
    } finally {
      w.close()
    }
  }

  def nodeLabel(n: Node) = style match {
    case IDOnly => n.id()
    case SymbolOnly => n.symbolString()
    case IDAndSymbol =>
      val symbolString = n.symbolString()
      if (symbolString != "")
        s"${symbolString} [${n.id}]"
      else
        n.id
  }

  val maxWeight = SafeMath.safeMax(network.nodes.asScala.map(nodeWeight(_)))
  val minWeight = SafeMath.safeMin(network.nodes.asScala.map(nodeWeight(_)))

  def DOTcolor(n: Node) = {
    if (java.lang.Double.isNaN(nodeWeight(n)) ||
        java.lang.Double.isInfinite(nodeWeight(n))) {
      "white"
    } else {
      val blue = (127 * (nodeWeight(n) - minWeight) / (maxWeight - minWeight)).toInt
      "#%02x%02x%02x".format(0, 128 + blue, 128 + blue)
    }
  }

  def DOTattributes(n: Node) = {
    n.`type` match {
      case Network.mirnaType => s"""color=blue, fillcolor="${DOTcolor(n)}" """
      case Network.mrnaType => s"""color=green, fillcolor="${DOTcolor(n)}" """
      case _ => ""
    }
  }
}
