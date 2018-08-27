package t.viewer.server.network

import t.platform.mirna._
import t.platform.Probe
import t.viewer.shared.network.Interaction
import t.viewer.server.matrix.ManagedMatrix
import t.viewer.shared.network.Node
import t.viewer.shared.network.Network
import scala.collection.JavaConversions._
import t.viewer.server.Platforms

object NetworkBuilder {

  /**
   * Uses the current target table to compute side table probes.
   * @param mainOffset defines the start of the current page in the main matrix.
   * @param mainSize defines the size of the current page in the main matrix.
   */
  def extractSideProbes(targets: TargetTable,
                        platforms: Platforms,
      main: ManagedMatrix,
      mainOffset: Int, mainSize: Int): Seq[String] = {
    val mainType = main.params.typ
    val mainSpecies = main.params.species
    val expPlatform = mainSpecies.expectedPlatform

    mainType match {
      case Network.mrnaType =>
        val domain = (main.current.orderedRowKeys drop mainOffset) take mainSize
        val range = targets.reverseTargets(platforms.resolve(domain))
        range.map(_._2.id).toSeq.distinct
      case Network.mirnaType =>
        val domain = (main.current.orderedRowKeys drop mainOffset) take mainSize
        val allProbes = platforms.data(expPlatform).toSeq
        val range = targets.targets(domain.map(new MiRNA(_)), allProbes)
        range.map(_._2.identifier).toSeq.distinct
      case _ => throw new Exception(s"Unable to extract side probes: unexpected column type $mainType for main table")
    }
  }
}

class NetworkBuilder(targets: TargetTable,
    platforms: Platforms,
    main: ManagedMatrix, side: ManagedMatrix) {

    lazy val mainType = main.params.typ

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

  def interactions(ints: Iterable[(MiRNA, Probe, Double, String)]) = {
    /* Future: construct label more intelligently, taking data source name
     * into account
     */
    for {
      iact <- ints;
      (mirna, probe, score, db) = iact;
      miLookup <- lookup(mirna); pLookup <- lookup(probe);
      label = TargetTable.interactionLabel(iact);
      int = new Interaction(miLookup, pLookup, label, score)
    } yield int
  }

  def interactionsForMirna(mirna: Iterable[MiRNA],
      platform: Iterable[Probe]) =
        interactions(targets.targets(mirna, platform))

  def interactionsForMrna(mrna: Iterable[Probe]) =
    interactions(targets.reverseTargets(mrna).map(x => (x._2, x._1, x._3, x._4)))

  import java.util.{ArrayList => JList}
  def build: Network = {
    if (main.info.numColumns() == 0) {
      return new Network("Network", new JList(), new JList())
    }

    val probes = platforms.resolve(main.current.orderedRowKeys take Network.MAX_SIZE)
    val rawInt = mainType match {
      case Network.mrnaType => interactionsForMrna(probes)
      case Network.mirnaType =>
        val pf = platforms.data.toSeq.flatMap(_._2)
        interactionsForMirna(probes.map(p => MiRNA(p.identifier)), pf)
    }
    val interactions = rawInt.toSeq.sortBy(_.weight()) take Network.MAX_SIZE
    new Network("Network", new JList(seqAsJavaList(nodes)),
        new JList(seqAsJavaList(interactions)))
  }
}
