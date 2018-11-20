package t.viewer.server.network

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.NetworkTestData
import t.viewer.server.matrix.ExtFoldBuilder
import t.viewer.server.Platforms
import t.db.testing.TestData
import t.common.shared.sample.Group
import t.viewer.server.Conversions._

@RunWith(classOf[JUnitRunner])
class ManagedNetworkTest extends TTestSuite {
  import NetworkTestData._
  import t.common.testing.TestData.groups

  NetworkTestData.populate()
  val dataSchema = t.common.testing.TestData.dataSchema
  val foldBuilder =  new ExtFoldBuilder(false, context.foldsDBReader,
      probes)
  
  test("basic") {
    val mirnaGroup = new Group(dataSchema, "mirnaGroup", mirnaSamples.map(s => asJavaSample(s)).toArray)
    val platforms = new Platforms(Map(mrnaPlatformId -> mrnaProbes.toSet, 
      mirnaPlatformId -> mirnaProbes.toSet))
    
    val main = foldBuilder.build(t.common.testing.TestData.groups take 5, false, true)
    val side = foldBuilder.build(Seq(mirnaGroup), false, true)
    val builder = new NetworkBuilder(targets, platforms, main, side)
    builder.build
  }
}