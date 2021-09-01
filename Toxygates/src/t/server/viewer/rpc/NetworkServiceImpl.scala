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

package t.server.viewer.rpc
import java.util.{List => JList}

import t.Context

import scala.collection.JavaConverters._
import t.shared.common.{GroupUtils, ValueType}
import t.shared.common.sample.Group
import t.intermine.{MiRNATargets, MiRawImporter}
import t.platform.mirna._
import t.platform.mirna.TargetTable
import t.sparql.ProbeStore
import t.gwt.viewer.client.rpc.NetworkService
import t.server.viewer.{CSVHelper, Configuration, MirnaSources, PlatformRegistry}
import t.server.viewer.Conversions._
import t.server.viewer.matrix.ControllerParams
import t.server.viewer.matrix.MatrixController
import t.server.viewer.network.NetworkController
import t.server.viewer.network.Serializer
import t.shared.viewer.Synthetic
import t.shared.viewer.TimeoutException
import t.shared.viewer.mirna.MirnaSource
import t.shared.viewer.network.Format
import t.shared.viewer.network.Network
import t.shared.viewer.network.NetworkInfo

object NetworkState {
  val stateKey = "network"
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

      //Make the count map in ManagedNetwork recalculate
      network.currentRowsChanged()
      //Make the count column update
      network.sideMatrix.reapplySynthetics()
      network.updateSideMatrix()
    }
  }

  /**
   * Networks will be stored here and also in the MatrixState's controllers.
   */
  var networks = Map[String, NetworkController]()
}

class NetworkServiceImpl extends StatefulServlet[NetworkState] with NetworkService {
  protected def stateKey = NetworkState.stateKey
  protected def newState = new NetworkState
  var config: Configuration = _

  def mirnaDir = context.config.data.mirnaDir
  lazy val netLoader = new NetworkLoader(context, platforms, mirnaDir)

  private def probeStore: ProbeStore = context.probeStore
  lazy val platforms = new PlatformRegistry(probeStore)

  override def localInit(c: Configuration) {
    super.localInit(c)
    config = c
  }

  @throws[TimeoutException]
  def setMirnaSources(sources: Array[MirnaSource]): scala.Unit = {
    var r = new TargetTableBuilder
    for (s <- sources; t <- netLoader.mirnaTargetTable(s)) {
      r.addAll(t)
    }

    getState().mirnaSources = sources
    getState().targetTable = r.build
    println(s"Session targetTable filtered to size ${getState().targetTable.size}")
  }

  /**
   * The main network loading operation.
   * Needs to load two matrices and also set up count columns
   * and the mapping between the two.
   */
  def loadNetwork(mainId: String, mainColumns: JList[Group], mainProbes: Array[String],
                  sideId: String, sideColumns: JList[Group], typ: ValueType,
                  mainPageSize: Int): NetworkInfo = {

    val scSideColumns = sideColumns.asScala
    val species = groupSpecies(scSideColumns(0))
    val netController = netLoader.load(getState.targetTable.speciesFilter(species),
      mainColumns.asScala, mainProbes,
      sideColumns.asScala, typ, mainPageSize)

    getState.controllers += (sideId -> netController.sideController)
    getState.controllers += mainId -> netController
    getState.networks += mainId -> netController
    netController.makeNetworkWithInfo
  }

  def currentView(mainTableId: String): Network =
    getState.networks(mainTableId).makeNetwork

  def distinctInteractions(net: Network): Network = {
    val all = net.interactions().asScala.groupBy(i => (i.from, i.to))
    val distinct = all.values.map(_.head)
    new Network(net.title(), net.nodes(),
      distinct.toList.asJava, net.wasTruncated(), net.trueSize())
  }

  def prepareNetworkDownload(mainTableId: String, format: Format,
                             messengerWeightColumn: String, microWeightColumn: String): String = {
    prepareNetworkDownload(distinctInteractions(currentView(mainTableId)),
      format, messengerWeightColumn, microWeightColumn)
  }

  def prepareNetworkDownload(network: Network, format: Format, messengerWeightColumn: String,
                             microWeightColumn: String): String = {
    val s = new Serializer(network, messengerWeightColumn, microWeightColumn)
    val file = CSVHelper.filename("toxygates", format.suffix)
    s.writeTo(s"${config.csvDirectory}/$file", format)
    s"${config.csvUrlBase}/$file"
  }
}

class NetworkLoader(context: Context, platforms: PlatformRegistry, mirnaDir: String) {
  def load(targetTable: TargetTable,
           mainColumns: Seq[Group], mainInitProbes: Array[String],
           sideColumns: Seq[Group], valueType: ValueType,
           mainPageSize: Int): NetworkController = {

    val sideMatrix = MatrixController(context, sideColumns, Seq(), valueType)

    val sideType = GroupUtils.groupType(sideMatrix.groups.head)
    val sideIsMRNA = sideType == Network.mrnaType;
    //Always load the empty probe set(all probes), to be able to revert to this view.
    //We optionally filter probes below.
    val netControllerParams = ControllerParams(mainColumns, Seq(), valueType)

    //The network controller (actually the managed network) will ensure that
    //the side matrix stays updated when the main matrix changes
    val net = new NetworkController(context, platforms, netControllerParams,
      sideMatrix, targetTable, mainPageSize, sideIsMRNA)

    val mainMatrix = net.managedMatrix
    if (mainInitProbes.nonEmpty) {
      mainMatrix.selectProbes(mainInitProbes)
    }

    sideMatrix.managedMatrix.addSynthetic(
      new Synthetic.Precomputed("Count", s"Number of times each $sideType appeared in the set",
        mainMatrix.currentViewCountMap, null))
    net
  }

  protected def tryReadTargetTable(file: String, doRead: String => TargetTable) =
    try {
      println(s"Try to read $file")
      val t = doRead(file)
      println(s"Read ${t.size} miRNA targets from $file")
      Some(t)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }

  lazy val mirdbTable =
    tryReadTargetTable(
      s"$mirnaDir/mirdb_filter.txt",
      new MiRDBConverter(_, "MiRDB 5.0").makeTable)

  lazy val mirtarbaseTable =
    tryReadTargetTable(
      s"$mirnaDir/tm_mirtarbase.txt",
      MiRNATargets.tableFromFile(_))

  lazy val miRawTable = {
    val allTranscripts = platforms.allProbes.iterator.flatMap(_.transcripts).toSet

    tryReadTargetTable(
      s"$mirnaDir/miraw_hsa_targets.txt",
      MiRawImporter.makeTable("MiRaw 6_1_10_AE10 NLL", _, allTranscripts))
  }

  import MirnaSources._
  def mirnaTargetTable(source: MirnaSource): Option[TargetTable] = {
    val table = source.id match {
      case MIRDB_SOURCE => mirdbTable
      case TARGETMINE_SOURCE => mirtarbaseTable
      case MIRAW_SOURCE => miRawTable
      case _ => throw new Exception("Unexpected MiRNA source")
    }
    table match {
      case Some(t) =>
        Option(source.limit) match {
          case Some(l) => Some(t.scoreFilter(l))
          case _       => Some(t)
        }
      case None =>
        Console.err.println(s"MirnaSource target table unavailable for ${source.id}")
        None
    }
  }
}


