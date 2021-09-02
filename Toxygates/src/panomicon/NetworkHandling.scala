package panomicon

import panomicon.json.NetworkParams
import t.Context
import t.platform.mirna.TargetTableBuilder
import t.server.viewer.rpc.NetworkLoader
import t.shared.common.ValueType
import t.shared.viewer.network.{Interaction, Network}
import ujson.Value
import upickle.default.writeJs

/**
 * Routines that support network loading requests
 */
class NetworkHandling(context: Context, matrixHandling: MatrixHandling) {
  lazy val netLoader = new NetworkLoader(context, context.config.data.mirnaDir)

  def loadNetwork(valueType: ValueType, pageSize: Int, netParams: NetworkParams): Network = {

    var r = new TargetTableBuilder
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
      sideGroups, valueType, pageSize)
    netController.makeNetwork
  }

  def interactionsToJson(ints: Iterable[Interaction]): Seq[Value] = {
    ints.toSeq.map(i => {
      writeJs(Map(
        "from" -> writeJs(i.from().id()),
        "to" -> writeJs(i.to().id()),
        "label" -> writeJs(i.label()),
        "weight" -> writeJs(i.weight().doubleValue())
      ))
    })
  }

}
