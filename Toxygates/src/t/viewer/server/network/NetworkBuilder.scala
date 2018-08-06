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

  final def lookup(p: Probe) = nodeLookup(p.identifier)
  final def lookup(m: MiRNA) = nodeLookup(m.id)

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
    targets.targets(mirna, platform).map { case (mirna, probe, score) =>
      new Interaction(lookup(mirna), lookup(probe), label, score) }
  }

  def interactionsForMrna(mrna: Iterable[Probe]) = {
    val label = ""
    targets.reverseTargets(mrna).map { case (probe, mirna, score) =>
      new Interaction(lookup(mirna), lookup(probe), label, score) }
  }

  import java.util.{ArrayList => JList}
  def build: Network = {
    val probes = platforms.resolve(main.current.orderedRowKeys take Network.MAX_SIZE)
    val interactions = interactionsForMrna(probes).toSeq.sortBy(_.weight()) take Network.MAX_SIZE
    new Network("Network", new JList(seqAsJavaList(nodes)),
        new JList(seqAsJavaList(interactions)))
  }
}
