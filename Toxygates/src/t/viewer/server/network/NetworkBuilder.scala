package t.viewer.server.network

import t.platform.mirna._
import t.platform.Probe
import t.viewer.shared.network.Interaction
import t.viewer.server.matrix.ManagedMatrix
import t.viewer.shared.network.Node
import t.viewer.shared.network.Network
import scala.collection.JavaConversions._
import t.viewer.server.Platforms

class NetworkBuilder(targets: TargetTable,
    platforms: Platforms,
    main: ManagedMatrix, side: ManagedMatrix) {

  //TODO do not hardcode types of nodes here
  val sourceNodes = getNodes(side, Network.mirnaType, Some(Network.MAX_SIZE))
  val destNodes = getNodes(main, Network.mrnaType, Some(Network.MAX_SIZE))
  val nodes = sourceNodes ++ destNodes
  val nodeLookup = Map() ++ nodes.map(n => n.id() -> n)

   def getNodes(mat: ManagedMatrix, mtype: String, maxSize: Option[Int]): Seq[Node] = {
    val allRows = mat.current.asRows
    val useRows = maxSize match {
      case Some(n) => allRows take n
      case None => allRows
    }
    useRows.map(r => Node.fromRow(r, mtype, mat.info))
  }

  def interactionsForMirna(mirna: Iterable[MiRNA],
      platform: Iterable[Probe]) = {
    val label = ""
    val weight = 1d
    targets.targets(mirna, platform).map { case (mirna, probe) =>
      new Interaction(nodeLookup(mirna.id), nodeLookup(probe.identifier),
          label, weight) }
  }

  def interactionsForMrna(mrna: Iterable[Probe]) = {
    val label = ""
    val weight = 1d
    targets.reverseTargets(mrna).map { case (probe, mirna) =>
      new Interaction(nodeLookup(mirna.id), nodeLookup(probe.identifier),
          label, weight) }
  }

  def build: Network = {
    val probes = platforms.resolve(main.current.orderedRowKeys take Network.MAX_SIZE)
    val interactions = interactionsForMrna(probes).toSeq.sortBy(_.weight()) take Network.MAX_SIZE
    new Network("Network", seqAsJavaList(nodes), interactions)
  }
}
