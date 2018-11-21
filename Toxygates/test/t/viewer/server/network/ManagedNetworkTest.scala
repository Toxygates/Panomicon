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

  test("controller") {
    val side = mirnaBuilder.build(Seq(mirnaGroup), false, true)
    val params = ControllerParams(context, platforms, mainGroups, Seq(),
      Seq(mrnaPlatformId), ValueType.Folds, false)
    val mainPageSize = 100
    val netCon = new NetworkController(params, side, targets, platforms, mainPageSize,
      false)
    val main = netCon.managedMatrix
    val expMainNodes = main.current.asRows take Network.MAX_NODES
    assert(expMainNodes.map(_.getProbe).toSet.subsetOf(mrnaIds.toSet))
    main.current.asRows.size should equal(mainPageSize)
    
    //Check that the edited side table agrees with what the target table says
    val expSideTargets = targets.reverseTargets(platforms.resolve(expMainNodes.map(_.getProbe)))
    val mirnas = expSideTargets.map(_._2).map(_.id)
    val sideNodes = side.current.asRows.map(_.getProbe)
    assert(sideNodes.toSet.subsetOf(mirnas.toSet))
    
  }
}