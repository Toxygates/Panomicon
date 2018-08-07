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
  //TODO handling of max size and informing user of cutoff
  val sourceNodes = getNodes(side, Network.mirnaType, Some(Network.MAX_SIZE))
  val destNodes = getNodes(main, Network.mrnaType, Some(Network.MAX_SIZE))
  val nodes = sourceNodes ++ destNodes
  val nodeLookup = Map() ++ nodes.map(n => n.id() -> n)

  final def lookup(p: Probe) = nodeLookup.get(p.identifier)
  final def lookup(m: MiRNA) = nodeLookup.get(m.id)

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
    //TODO labels
    val label = ""
    for {
      (mirna, probe, score) <- targets.targets(mirna, platform);
      miLookup <- lookup(mirna); pLookup <- lookup(probe);
      int = new Interaction(miLookup, pLookup, label, score)
    } yield int    
  }

  def interactionsForMrna(mrna: Iterable[Probe]) = {
    //TODO labels
    val label = ""
    for {
      (probe, mirna, score) <- targets.reverseTargets(mrna);
      miLookup <- lookup(mirna); pLookup <- lookup(probe);
      int = new Interaction(miLookup, pLookup, label, score)
    } yield int    
  }

  import java.util.{ArrayList => JList}
  def build: Network = {
    val probes = platforms.resolve(main.current.orderedRowKeys take Network.MAX_SIZE)
    val interactions = interactionsForMrna(probes).toSeq.sortBy(_.weight()) take Network.MAX_SIZE
    new Network("Network", new JList(seqAsJavaList(nodes)),
        new JList(seqAsJavaList(interactions)))
  }
}
