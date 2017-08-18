package t.platform

import t.TTestSuite
import otg.testing.TestData
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import otg.model.sample.Attribute._

@RunWith(classOf[JUnitRunner])
class ControlGroupTest extends TTestSuite {
  val metadata = TestData.metadata
  import t.db.testing.TestData.enumValues
  val bioParams = TestData.bioParameters
  val samples = t.db.testing.TestData.samples

  val liverParam = metadata.attributes.byId("liver_wt")
  val kidneyParam = metadata.attributes.byId("kidney_total_wt")

  test("basic") {
    for (s <- samples) {
      val cg = TestData.controlGroups(s)
      cg.samples.size should equal(3)

      val remove = Set(DoseLevel.id, Individual.id)

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

      if (isControl || s.sampleClass(Individual.id) == "2") {
         //healthy
        metadata.parameter(s, "liver_wt").get.toDouble should
          be (3.0 +- 0.1)
      } else {
        //unhealthy
        metadata.parameter(s, "liver_wt").get.toDouble should
          be (5.0 +- 0.1)
      }

      val time = metadata.parameter(s, ExposureTime).get
      println(cg.paramVals)

      //TODO might need to adjust these limits
      cg.lowerBound(liverParam, 1).get should
        be (2.9 +- 0.2)
      cg.upperBound(liverParam, 1).get should
        be (3.1 +- 0.2)
    }
  }
}
