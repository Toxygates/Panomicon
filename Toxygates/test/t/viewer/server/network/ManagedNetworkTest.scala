package t.viewer.server.network

import scala.collection.JavaConverters._

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import t.TTestSuite
import t.common.shared.ValueType
import t.common.shared.sample.Group
import t.db.testing.NetworkTestData
import t.viewer.server.Conversions._
import t.viewer.server.Platforms
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
  val platforms = new Platforms(Map(
    mrnaPlatformId -> mrnaProbes.toSet,
    mirnaPlatformId -> mirnaProbes.toSet))

  val mainGroups = t.common.testing.TestData.groups take 5

  test("basic") {
    val main = mrnaBuilder.build(mainGroups, false, true)
    val side = mirnaBuilder.build(Seq(mirnaGroup), false, true)
    val builder = new NetworkBuilder(targets, platforms, main, side)
    val network = builder.build

    val expMainNodes = main.current.asRows take Network.MAX_NODES
    assert(expMainNodes.map(_.getProbe).toSet.subsetOf(mrnaIds.toSet))

    val expSideNodes = side.current.asRows take Network.MAX_NODES
    assert(expSideNodes.map(_.getProbe).toSet.subsetOf(mirnaIds.toSet))

    val ids = (expMainNodes.toSeq ++ expSideNodes).map(_.getProbe)

    network.nodes.asScala.map(_.id).toSet should equal(ids.toSet)
  }

  def checkNetworkInvariants(main: ManagedNetwork, side: ManagedMatrix) {
    println(s"Checking network with ${main.current.rows} rows, side ${side.current.rows}, targets ${main.targets.size}")
    //getPageView updates the side matrix based on the new view
    var probes = main.getPageView(0, 100).map(_.getProbe)
    checkSideTable(probes, main.targets, side)
    probes = main.getPageView(100, 100).map(_.getProbe)
    checkSideTable(probes, main.targets, side)
    probes = main.getPageView(500, 100).map(_.getProbe)
    checkSideTable(probes, main.targets, side)
  }

  def checkSideTable(mainProbes: Seq[String], targets: TargetTable, side: ManagedMatrix) {
    val sideProbes = side.current.asRows.map(_.getProbe)

    val expSideTargets = targets.reverseTargets(platforms.resolve(mainProbes))
    checkEqualSets(sideProbes.toSet, expSideTargets.map(_._2.id).toSet)

    checkSubset(mainProbes.toSet, mrnaIds.toSet)
    checkSubset(sideProbes.toSet, mirnaIds.toSet)
  }

  test("controller") {
    val side = mirnaBuilder.build(Seq(mirnaGroup), false, true)
    val params = ControllerParams(context, platforms, mainGroups, Seq(),
      Seq(mrnaPlatformId), ValueType.Folds, false)
    val mainPageSize = 100
    val netCon = new NetworkController(params, side, targets, platforms, mainPageSize,
      false)
    val main = netCon.managedMatrix

    //Check that the side table - main table correspondence agrees with what the target table says
    checkNetworkInvariants(main, side)

    val subset = mrnaIds take 100
    main.selectProbes(subset)
    checkNetworkInvariants(main, side)

    main.resetSortAndFilter()
    main.targets = targets.scoreFilter(90)
    //Propagate the new target table
    main.updateSideMatrix()
    assert(main.targets.size > 0)
    assert(main.targets.size != targets.size)
    checkNetworkInvariants(main, side)

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
