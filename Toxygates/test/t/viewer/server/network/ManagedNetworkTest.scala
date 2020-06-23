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

import scala.collection.JavaConverters._

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner

import t.viewer.server.rpc.Conversions._
import t.TTestSuite
import t.common.shared.ValueType
import t.common.shared.sample.Group
import t.db.testing.NetworkTestData
import t.platform.mirna._
import t.viewer.server.Conversions._
import t.viewer.server.PlatformRegistry
import t.viewer.server.matrix.ControllerParams
import t.viewer.server.matrix.ExtFoldBuilder
import t.viewer.shared.network.Network
import t.viewer.shared.ColumnFilter
import t.viewer.server.matrix.ManagedMatrix
import t.platform.mirna.TargetTable

@RunWith(classOf[JUnitRunner])
class ManagedNetworkTest extends TTestSuite {
  import t.common.testing.TestData.groups
  import t.db.testing.NetworkTestData._

  NetworkTestData.populate()
  val dataSchema = t.common.testing.TestData.dataSchema
  val mrnaBuilder = new ExtFoldBuilder(false, context.foldsDBReader,
    mrnaIds)
  val mirnaBuilder = new ExtFoldBuilder(false, context.foldsDBReader,
    mirnaIds)

  val mirnaGroup = new Group(dataSchema, "mirnaGroup", mirnaSamples.map(s => asJavaSample(s)).toArray)
  val platforms = new PlatformRegistry(Map(
    t.db.testing.DBTestData.mrnaPlatformId -> mrnaProbes.toSet,
    mirnaPlatformId -> mirnaProbes.toSet))

  val mrnaGroups = t.common.testing.TestData.groups take 5

  test("basic") {
    val main = mrnaBuilder.build(mrnaGroups, false, true)
    val side = mirnaBuilder.build(Seq(mirnaGroup), false, true)
    val builder = new NetworkBuilder(targets, platforms, main, side)
    val network = builder.build

    val expMainNodes = main.current.asRows take Network.MAX_NODES
    assert(expMainNodes.map(_.getProbe).toSet.subsetOf(mrnaIds.toSet))

    val expSideNodes = side.current.asRows
    assert(expSideNodes.map(_.getProbe).toSet.subsetOf(mirnaIds.toSet))

    val ids = (expMainNodes.toSeq ++ expSideNodes).map(_.getProbe)
    network.nodes.asScala.map(_.id).toSet should equal(ids.toSet)
  }

  def checkNetworkInvariants(main: ManagedNetwork, side: ManagedMatrix,
      reverseLookup: Boolean) {
    println(s"Checking network with ${main.current.rows} rows, side ${side.current.rows}, targets ${main.targets.size}")
    //getPageView updates the side matrix based on the new view
    var probes = main.getPageView(0, 100).map(_.getProbe)
    checkSideTable(probes, main.targets, side, reverseLookup)
    probes = main.getPageView(100, 100).map(_.getProbe)
    checkSideTable(probes, main.targets, side, reverseLookup)
    probes = main.getPageView(500, 100).map(_.getProbe)
    checkSideTable(probes, main.targets, side, reverseLookup)
  }

  def checkSideTable(mainProbes: Seq[String], targets: TargetTable, side: ManagedMatrix,
      reverseLookup: Boolean) {
    val sideProbes = side.current.asRows.map(_.getProbe)

    val expSideTargets = if (reverseLookup) {
      targets.reverseTargets(platforms.resolve(mainProbes)).map(_._2.id)
    } else {
      targets.targets(mainProbes.map(MiRNA(_)), mrnaProbes).map(_._2.identifier)
    }
    checkEqualSets(sideProbes.toSet, expSideTargets.toSet)

    val (mainSet, sideSet) =
      if (reverseLookup) (mrnaIds.toSet, mirnaIds.toSet)
      else (mirnaIds.toSet, mrnaIds.toSet)

    checkSubset(mainProbes.toSet, mainSet)
    checkSubset(sideProbes.toSet, sideSet)
  }

  test("forward network") {
    val side = mirnaBuilder.build(Seq(mirnaGroup), false, true)
    networkTest(side, mrnaGroups, t.db.testing.DBTestData.mrnaPlatformId, true)
  }

  test("reverse network") {
    val side = mrnaBuilder.build(mrnaGroups, false, true)
    networkTest(side, Seq(mirnaGroup), mirnaPlatformId, false)
  }

  def networkTest(side: ManagedMatrix, mainGroups: Seq[Group],
      mainPlatform: String, reverseLookup: Boolean) {
    val params = ControllerParams(context, platforms, mainGroups, Seq(),
      Seq(mainPlatform), ValueType.Folds, false)
    val mainPageSize = 100
    val netCon = new NetworkController(params, side, targets, platforms, mainPageSize,
      false)
    val main = netCon.managedMatrix

    //Check that the side table - main table correspondence agrees with what the target table says
    checkNetworkInvariants(main, side, reverseLookup)

    val subset = mrnaIds take 100
    main.selectProbes(subset)
    checkNetworkInvariants(main, side, reverseLookup)

    main.resetSortAndFilter()
    main.targets = targets.scoreFilter(90)
    //Propagate the new target table
    main.updateSideMatrix()
    assert(main.targets.size > 0)
    assert(main.targets.size != targets.size)
    checkNetworkInvariants(main, side, reverseLookup)

    main.resetSortAndFilter()
  }

  def checkEqualSets[T](x: Set[T], y: Set[T]) {
    if (x != y) {
      println("Two sets were not equal. Added in LHS:")
      println(x -- y)
      println("Added in RHS:")
      println(y -- x)
      assert(false)
    }
  }

  def checkSubset[T](sub: Set[T], main: Set[T]) {
    if (!sub.subsetOf(main)) {
      println("Sub was not a subset of main, new elements:")
      println(sub -- main)
      assert(false)
    }
  }
}
