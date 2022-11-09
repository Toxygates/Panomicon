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

package t.server.viewer.network

import t.server.common.GWTUtils._
import t.platform.{PlatformRegistry, Probe}
import t.platform.mirna._
import t.server.viewer.matrix.{ExpressionMatrix, ManagedMatrix, PageDecorator}
import t.shared.viewer.network.Interaction
import t.shared.viewer.network.Network
import t.shared.viewer.network.Node

import scala.collection.mutable.{Set => MSet}
import t.shared.viewer.ManagedMatrixInfo
import t.shared.common.GWTTypes

object NetworkBuilder {
  /**
   * Uses the current target table to compute side table probes.
   * @param mainOffset defines the start of the current page in the main matrix.
   * @param mainSize defines the size of the current page in the main matrix.
   */
  def extractSideProbes(targets: TargetTable,
                        platforms: PlatformRegistry,
                        main: ManagedMatrix,
                        side: ManagedMatrix,
                        mainOffset: Int, mainSize: Int): Seq[String] = {
    val mainType = main.params.typ
    val mainPlatform = main.params.platform
    val sidePlatform = side.params.platform

    mainType match {
      case Network.mrnaType =>
        val domain = main.current.rowKeys.slice(mainOffset, mainOffset + mainSize)
        val range = targets.reverseTargets(platforms.resolve(mainPlatform, domain))
        range.map(_._2.id).toSeq.distinct
      case Network.mirnaType =>
        val domain = main.current.rowKeys.slice(mainOffset, mainOffset + mainSize)
        val allProbes = platforms.platformProbes(sidePlatform).toSeq
        val range = targets.targetsForPlatform(domain.map(new MiRNA(_)), allProbes)
        range.map(_._2.identifier).toSeq.distinct
      case _ => throw new Exception(s"Unable to extract side probes: unexpected column type $mainType for main table")
    }
  }
}

class NetworkBuilder(targets: TargetTable,
                     platforms: PlatformRegistry,
                     main: ManagedMatrix, side: ManagedMatrix) {
  import GWTTypes._

  val mainType = main.params.typ
  val sideType = side.params.typ
  val mainInfo = main.info
  val sideInfo = side.info

  /**
   * Extract all nodes of a given type from the given ExprMatrix.
   */
  def getNodes(mat: ExpressionMatrix, info: ManagedMatrixInfo, mtype: String, maxSize: Option[Int]): Seq[Node] = {
    val allRows = PageDecorator.asGWT(mat.asRows)
    val useRows = maxSize match {
      case Some(n) => allRows take n
      case None    => allRows
    }
    useRows.map(r => {
      val probe = r.getProbe
      val symbols = platforms.getProbe(main.params.platform, probe).toList.flatMap(_.symbols).asGWT
      Node.fromRow(r, symbols, mtype, info)
    })
  }

  def targetsForMirna(mirna: Iterable[MiRNA], targetPlatform: Iterable[Probe]) =
    targets.targetsForPlatform(mirna, targetPlatform)

  def targetsForMrna(mrna: Iterable[Probe]) =
    targets.reverseTargets(mrna).map(x => (x._2, x._1, x._3, x._4))

  def targetSideProbe(t: (MiRNA, Probe, _, _)) = sideType match {
    case Network.mrnaType => t._2.identifier
    case Network.mirnaType => t._1.id
  }

  def probeTargets(probes: Seq[Probe], targetPlatform: Seq[Probe]) = mainType match {
     case Network.mrnaType => targetsForMrna(probes)
      case Network.mirnaType =>
        targetsForMirna(probes.map(p => MiRNA(p.identifier)), targetPlatform)
  }

  /**
   * Construct a network from the given main and side sub-matrices
   */
  def networkFromSelection(mainSel: ExpressionMatrix, sideSel: ExpressionMatrix,
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

    //In case there are too many interactions,
    //we might prioritise by weight here and limit the number.
    //Currently edges/interactions are not limited.
    //val interactions = r.flatMap(_._2) //.toSeq.sortBy(_.weight()) take Network.MAX_EDGES
    new Network("Network", nodes.asGWT, ints.asGWT)
  }

  def build: Network = {
    if (main.info.numColumns() == 0) {
      return new Network("Network", mkList(), mkList())
    }

    val mainPlatform = main.params.platform
    val sidePlatform = side.params.platform
    val sidePlatformProbes = platforms.platformProbes(sidePlatform).toSeq

    val mainTargets = probeTargets(platforms.resolve(mainPlatform, main.current.rowKeys), sidePlatformProbes)
    val sideTableProbeSet = side.rawGrouped.rowKeys.toSet
    val sideProbes = mainTargets.map(targetSideProbe).toSeq.distinct.
      filter(sideTableProbeSet.contains)

    //Select as a new copy, in order to avoid affecting the current view being
    //displayed
    val sideSel = side.selectProbesAsCopy(sideProbes.toSeq)

    networkFromSelection(main.current, sideSel.current, mainTargets)
  }
}
