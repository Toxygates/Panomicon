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
import t.common.shared.GWTTypes

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
  import GWTTypes._

  val mainType = main.params.typ
  val sideType = side.params.typ
  val mainInfo = main.info
  val sideInfo = side.info

  /**
   * Extract all nodes of a given type from the given ExprMatrix.
   */
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

  def targetsForMirna(mirna: Iterable[MiRNA], platform: Iterable[Probe]) =
    targets.targets(mirna, platform)

  def targetsForMrna(mrna: Iterable[Probe]) =
    targets.reverseTargets(mrna).map(x => (x._2, x._1, x._3, x._4))

  def targetSideProbe(t: (MiRNA, Probe, _, _)) = sideType match {
    case Network.mrnaType => t._2.identifier
    case Network.mirnaType => t._1.id
  }

  def probeTargets(probes: Seq[Probe], allPlatforms: Seq[Probe]) = mainType match {
     case Network.mrnaType => targetsForMrna(probes)
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

  /**
   * Pick the top rows that have interactions from the given matrix,
   * padding with non-interacting rows if needed
   */
  def topProbesWithInteractions(targets: Iterable[(MiRNA, Probe)]) = {

    var haveInteractions = MSet[String]()
    //Track which probes (on both sides) have interactions
    for ((mirna, mrna) <- targets) {
      haveInteractions += mirna.id
      haveInteractions += mrna.identifier
    }
    var count = 0
    val max = Network.MAX_NODES

    //Preserve the sort order while taking at most MAX_NODES nodes with interactions
    var keepMainNodes = Set[String]()
    for {
      n <- main.current.orderedRowKeys
      if (count < max)
      if (haveInteractions.contains(n))
    } {
      count += 1
      keepMainNodes += n
    }

    //Extend the main node set if too few nodes had interactions
    if (keepMainNodes.size < max) {
      val need = max - keepMainNodes.size
      keepMainNodes ++= main.current.orderedRowKeys.filter(!keepMainNodes.contains(_)).take(need)
    }
    keepMainNodes.toSeq
  }

  def build: Network = {
    if (main.info.numColumns() == 0) {
      return new Network("Network", mkList(), mkList(), false, 0)
    }

    val probes = platforms.resolve(main.current.orderedRowKeys)
    val pfs = platforms.data.toSeq.flatMap(_._2)
    val allTargets = probeTargets(probes, pfs)

    val keepNodes = topProbesWithInteractions(allTargets.map(x => (x._1, x._2)))
    val mainSel = main.current.selectNamedRows(keepNodes)
    val mainTargets = probeTargets(platforms.resolve(mainSel.orderedRowKeys), pfs)
    val sideTableProbeSet = side.rawGrouped.rowKeys.toSet
    val sideProbes = mainTargets.map(targetSideProbe).toSeq.distinct.
      filter(sideTableProbeSet.contains(_))

    //Select as a new copy, in order to avoid affecting the current view being
    //displayed
    val sideSel = side.selectProbesAsCopy(sideProbes.toSeq)

    networkFromSelection(mainSel, sideSel.current, mainTargets)
  }
}
