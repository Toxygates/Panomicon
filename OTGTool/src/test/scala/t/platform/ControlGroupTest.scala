package t.platform

import t.TTestSuite
import otg.testing.TestData
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import otg.model.sample.OTGAttribute._

@RunWith(classOf[JUnitRunner])
class ControlGroupTest extends TTestSuite {
  val metadata = TestData.metadata
  import t.db.testing.TestData.enumValues
  val bioParams = TestData.bioParameters
  val samples = t.db.testing.TestData.samples

  test("basic") {
    for (s <- samples) {
      val cg = TestData.controlGroups(s)
      cg.samples.size should equal(3)

      val remove = Set(DoseLevel, Individual, LiverWeight, KidneyWeight)

      //Except for the removed keys, the sample classes of each sample in the control
      //group should be equal to the sample class of s.
      for (cs <- cg.samples) {
        cs.sampleClass.getMap.filter(c => ! remove.contains(c._1)) should
          equal(s.sampleClass.getMap.filter(c => ! remove.contains(c._1)))
      }
    }
  }

  test("liver weight") {
    for (s <- samples;
      cg = TestData.controlGroups(s)) {
      val isControl = cg.samples.toSet.contains(s)

      if (isControl || s.sampleClass(Individual) == "2") {
         //healthy
        metadata.sampleAttribute(s, LiverWeight).get.toDouble should
          be (3.0 +- 0.1)
      } else {
        //unhealthy
        metadata.sampleAttribute(s, LiverWeight).get.toDouble should
          be (5.0 +- 0.1)
      }

      val time = metadata.sampleAttribute(s, ExposureTime).get
      println(cg.paramVals)

      /*
       * These limits may need to be adjusted in the future.
       */
      cg.lowerBound(LiverWeight, 1).get should
        be (2.9 +- 0.2)
      cg.upperBound(LiverWeight, 1).get should
        be (3.1 +- 0.2)
    }
  }
}
