package panomicon

import panomicon.json.NetworkParams
import t.Context
import t.platform.mirna.TargetTableBuilder
import t.server.viewer.rpc.NetworkLoader
import t.shared.common.ValueType
import t.shared.viewer.network.{Interaction, Network, Node}
import ujson.Value
import upickle.default._
import scala.collection.JavaConverters._

/**
 * Routines that support network loading requests
 */
class NetworkHandling(context: Context, matrixHandling: MatrixHandling) {
  lazy val netLoader = new NetworkLoader(context, context.config.data.mirnaDir)

  def loadNetwork(valueType: ValueType, netParams: NetworkParams): Network = {

    val r = new TargetTableBuilder
    for (t <- netLoader.mirnaTargetTable(netParams.mirnaSource)) {
      r.addAll(t)
    }
    val targetTable = r.build

    println(s"Constructed target table of size ${targetTable.size}")
    if (targetTable.isEmpty) {
      println("Warning: the target table is empty, no networks can be constructed.")
    }

    val mainGroups = matrixHandling.filledGroups(netParams.matrix1)
    val mainInitProbes = netParams.matrix1.probes
    val sideGroups = matrixHandling.filledGroups(netParams.matrix2)
    val netController = netLoader.load(targetTable, mainGroups, mainInitProbes.toArray,
      sideGroups, valueType)
    netController.makeNetwork
  }

  def networkToJson(n: Network): Map[String, Value] =
    Map(
      "nodes" -> nodesToJson(n.nodes().asScala),
      "interactions" -> interactionsToJson(n.interactions().asScala)
    )

  def interactionsToJson(ints: Seq[Interaction]): Seq[Value] = {
    ints.map(i => writeJs(Map(
        "from" -> writeJs(i.from().id()),
        "to" -> writeJs(i.to().id()),
        "label" -> writeJs(i.label()),
        "weight" -> writeJs(i.weight().doubleValue())
      ))
    )
  }

  private def normalizeNodeType(t: String) = t match {
    case "miRNA" => "microRNA"
    case _ => t
  }

  def nodesToJson(nodes: Seq[Node]): Seq[Value] = {
    nodes.map(n => writeJs(
      Map(
      "id" -> writeJs(n.id()),
      "type" -> writeJs(normalizeNodeType(n.`type`())),
      "weights" -> writeJs(
        n.weights().asScala.toMap.map(x => x._1 -> writeJs[Double](x._2))),
      "symbols" -> writeJs(n.symbols().asScala)
    )
    ))
  }


}
