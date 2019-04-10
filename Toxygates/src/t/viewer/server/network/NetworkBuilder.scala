package t.viewer.server.network

import t.common.server.GWTUtils._
import t.platform.Probe
import t.platform.mirna._
import t.viewer.server.Platforms
import t.viewer.server.matrix.ManagedMatrix
import t.viewer.shared.network.Interaction
import t.viewer.shared.network.Network
import t.viewer.shared.network.Node
import scala.collection.mutable.{Set => MSet}
import t.viewer.server.matrix.ExprMatrix
import t.viewer.shared.ManagedMatrixInfo

object NetworkBuilder {

  /**
   * Uses the current target table to compute side table probes.
   * @param mainOffset defines the start of the current page in the main matrix.
   * @param mainSize defines the size of the current page in the main matrix.
   */
  def extractSideProbes(targets: TargetTable,
                        platforms: Platforms,
      main: ManagedMatrix,
      side: ManagedMatrix,
      mainOffset: Int, mainSize: Int): Seq[String] = {
    val mainType = main.params.typ
    val expPlatform = side.params.platform

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

  val mainType = main.params.typ
  val sideType = side.params.typ
  val mainInfo = main.info
  val sideInfo = side.info

  def getNodes(mat: ExprMatrix, info: ManagedMatrixInfo, mtype: String, maxSize: Option[Int]): Seq[Node] = {
    val allRows = mat.asRows
    val useRows = maxSize match {
      case Some(n) => allRows take n
      case None    => allRows
    }
    useRows.map(r => {
      val probe = r.getProbe
      val symbols = platforms.identifierLookup(probe).symbols.asGWT
      Node.fromRow(r, symbols, mtype, info)
    })
  }

  def targetsForMirna(mirna: Iterable[MiRNA],
      platform: Iterable[Probe]) =
        targets.targets(mirna, platform)

  def targetsForMrna(mrna: Iterable[Probe]) =
    targets.reverseTargets(mrna).map(x => (x._2, x._1, x._3, x._4))

  def targetSideProbe(t: (MiRNA, Probe, _, _)) = sideType match {
    case Network.mrnaType => t._2.identifier
    case Network.mirnaType => t._1.id
  }

  def probeTargets(probes: Seq[Probe], allPlatforms: Seq[Probe]) = mainType match {
     case Network.mrnaType =>
        targetsForMrna(probes)
      case Network.mirnaType =>
        val pf = platforms.data.toSeq.flatMap(_._2)
        targetsForMirna(probes.map(p => MiRNA(p.identifier)), pf)
  }

  /**
   * Construct a network from the given main and side sub-matrices
   */
  def networkFromSelection(mainSel: ExprMatrix, sideSel: ExprMatrix,
      targets: Iterable[(MiRNA, Probe, Double, String)]) = {

    val mainNodes = getNodes(mainSel, mainInfo, mainType, None)
    val sideNodes = getNodes(sideSel, sideInfo, sideType, None)

    val nodes = mainNodes ++ sideNodes

    val nodeLookup = Map() ++ nodes.map(n => n.id -> n)
    def lookup(p: Probe) = nodeLookup.get(p.identifier)
    def lookupMicro(m: MiRNA) = nodeLookup.get(m.id)

    val ints = (for {
        iact <- targets
        (mirna, probe, score, label) = iact
        miLookup <- lookupMicro(mirna); pLookup <- lookup(probe)
        int = new Interaction(miLookup, pLookup, label, score)
      } yield int)

    val truncated = (mainSel.rows < main.current.rows)
    val trueSize = main.current.rows

    //In case there are too many interactions,
    //we might prioritise by weight here and limit the number.
    //Currently edges/interactions are not limited.
    //val interactions = r.flatMap(_._2) //.toSeq.sortBy(_.weight()) take Network.MAX_EDGES
    new Network("Network", nodes.asGWT, ints.asGWT,
      truncated, trueSize)
  }

  import java.util.{ ArrayList => JList }
  def build: Network = {
    if (main.info.numColumns() == 0) {
      return new Network("Network", new JList(), new JList(), false, 0)
    }

    var haveInteractions = MSet[String]()
    val probes = platforms.resolve(main.current.orderedRowKeys)
    val pfs = platforms.data.toSeq.flatMap(_._2)
    val allTargets = probeTargets(probes, pfs)

    //Track which probes (on both sides) have interactions
    for ((mirna, mrna, _, _) <- allTargets) {
      haveInteractions += mirna.id
      haveInteractions += mrna.identifier
    }

    var count = 0

    //Preserve the sort order while taking at most MAX_NODES nodes with interactions
    var keepMainNodes = Set[String]()
    for {
      n <- main.current.orderedRowKeys
      if (count < Network.MAX_NODES)
      if (haveInteractions.contains(n))
    } {
      count += 1
      keepMainNodes += n
    }

    //Extend the main node set if too few nodes had interactions
    if (keepMainNodes.size < Network.MAX_NODES) {
      val need = Network.MAX_NODES - keepMainNodes.size
      keepMainNodes ++= main.current.orderedRowKeys.filter(!keepMainNodes.contains(_)).take(need)
    }
    val mainSel = main.current.selectNamedRows(keepMainNodes.toSeq)
    val mainTargets = probeTargets(platforms.resolve(mainSel.orderedRowKeys), pfs)
    val sideTableProbeSet = side.rawGrouped.rowKeys.toSet
    val sideProbes = mainTargets.map(targetSideProbe).toSeq.distinct.
      filter(sideTableProbeSet.contains(_))

    //Must select from rawGrouped since sideProbes may be larger than
    //side.current's probe set
    val sideSel = side.rawGrouped.selectNamedRows(sideProbes.toSeq)

    networkFromSelection(mainSel, sideSel, mainTargets)
  }
}
