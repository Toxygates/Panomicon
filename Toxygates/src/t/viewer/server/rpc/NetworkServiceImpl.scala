/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package t.viewer.server.rpc
import java.lang.{ Double => JDouble }
import java.util.{ HashMap => JHMap }
import java.util.{ List => JList }

import scala.collection.JavaConversions._

import t.common.shared.GroupUtils
import t.common.shared.ValueType
import t.common.shared.sample.Group
import t.platform.mirna._
import t.platform.mirna.TargetTable
import t.sparql.Probes
import t.viewer.client.rpc.NetworkService
import t.viewer.server.CSVHelper
import t.viewer.server.Configuration
import t.viewer.server.Conversions._
import t.viewer.server.matrix.ControllerParams
import t.viewer.server.matrix.ManagedMatrix
import t.viewer.server.matrix.MatrixController
import t.viewer.server.network.NetworkBuilder
import t.viewer.server.network.NetworkController
import t.viewer.server.network.Serializer
import t.viewer.shared.Synthetic
import t.viewer.shared.TimeoutException
import t.viewer.shared.mirna.MirnaSource
import t.viewer.shared.network.Format
import t.viewer.shared.network.Network
import t.viewer.shared.network.NetworkInfo

object NetworkState {
  val stateKey = "network"

  //Temporary location for this
  def buildCountMap(mat: ManagedMatrix,
    targetTable: TargetTable,
    platforms: t.viewer.server.Platforms,
    fromMiRNA: Boolean) = {
    val lookup = mat.current.rowKeys.toSeq

    //TODO filter by species etc
    if (fromMiRNA) {
      val gr = mat.currentInfo.columnGroup(0)
      val sp = t.viewer.server.Conversions.groupSpecies(gr)

      val all = platforms.data(sp.expectedPlatform)
      val targets = targetTable.targets(lookup.map(MiRNA(_)), all)
      targets.groupBy(_._2).map(x => (x._1.identifier, new JDouble(x._2.size)))
    } else {
      val resolved = platforms.resolve(lookup)
      val targets = targetTable.reverseTargets(resolved)
      targets.groupBy(_._2).map(x => (x._1.id, new JDouble(x._2.size)))
    }
  }
}

class NetworkState extends MatrixState {
  var mirnaSources: Array[MirnaSource] = Array()

  var _targetTable: TargetTable = new TargetTable(Array(), Array(), Array(), Array())

  def targetTable = synchronized { _targetTable }
  def targetTable_=(tt: TargetTable) = synchronized {
    _targetTable = tt

    for {
      net <- networks.values; network = net.managedMatrix
    } {
      network.targets = tt
      network.updateSideMatrix()
    }
  }

  /**
   * Networks will be stored here and also in the MatrixState's controllers.
   */
  var networks = Map[String, NetworkController]()
}

abstract class NetworkServiceImpl extends StatefulServlet[NetworkState] with NetworkService {
  protected def stateKey = NetworkState.stateKey
  protected def newState = new NetworkState
  var config: Configuration = _

  def mirnaDir = context.config.data.mirnaDir

  private def probeStore: Probes = context.probes
  lazy val platforms = t.viewer.server.Platforms(probeStore)

  override def localInit(c: Configuration) {
    config = c
  }

  @throws[TimeoutException]
  def setMirnaSources(sources: Array[MirnaSource]): scala.Unit = {
    getState().mirnaSources = sources
  }

  /**
   * The main network loading operation.
   * Needs to load two matrices and also set up count columns
   * and the mapping between the two.
   */
  def loadNetwork(mainId: String, mainColumns: JList[Group], mainProbes: Array[String],
    sideId: String, sideColumns: JList[Group], typ: ValueType,
    mainPageSize: Int): NetworkInfo = {

    //Orthologous mode is not supported for network loading
    val orthMappings = () => List()

    getState.controllers += (sideId ->
      MatrixController(context, orthMappings, sideColumns, Seq(), typ, false))
    val sideMat = getState.matrix(sideId)

    val gt = GroupUtils.groupType(sideColumns(0))
    val species = groupSpecies(sideColumns(0))

    var targets = getState.targetTable
    targets = targets.speciesFilter(species)

    val params = ControllerParams(context, mainColumns, mainProbes,
      MatrixController.groupPlatforms(context, mainColumns), typ, false)

    //The network controller (actually the managed network) will ensure that
    //the side matrix stays updated when the main matrix changes

    val net = new NetworkController(params, sideMat, targets, platforms, mainPageSize)
    getState.controllers += mainId -> net
    getState.networks += mainId -> net

    val mainMat = net.managedMatrix

    val fromMiRNA = gt == Network.mrnaType;
    val countMap = NetworkState.buildCountMap(mainMat, targets, platforms,
      fromMiRNA)
    val pset = sideMat.initProbes.toSet
    val filtered = countMap.filter(x => pset.contains(x._1))
    sideMat.addSynthetic(
      new Synthetic.Precomputed("Count", s"Number of times each $gt appeared",
        new JHMap(mapAsJavaMap(filtered)), null))

    //To do: init filters...
    new NetworkInfo(mainMat.info, sideMat.info, net.makeNetwork)
  }

  def currentView(mainTableId: String): Network =
    getState.networks(mainTableId).makeNetwork

  def prepareNetworkDownload(mainTableId: String, format: Format,
    messengerWeightColumn: String, microWeightColumn: String): String = {
    prepareNetworkDownload(currentView(mainTableId), format, messengerWeightColumn, microWeightColumn)
  }

  def prepareNetworkDownload(network: Network, format: Format, messengerWeightColumn: String,
    microWeightColumn: String): String = {
    val s = new Serializer(network, messengerWeightColumn, microWeightColumn)
    val file = CSVHelper.filename("toxygates", format.suffix)
    s.writeTo(s"${config.csvDirectory}/$file", format)
    s"${config.csvUrlBase}/$file"
  }

}
