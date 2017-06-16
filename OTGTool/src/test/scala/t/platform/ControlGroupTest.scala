package t.platform

import t.TTestSuite
import otg.testing.TestData

class ControlGroupTest extends TTestSuite {
  val metadata = TestData.metadata
  import t.db.testing.TestData.enumValues
  val bioParams = TestData.bioParameters
  val samples = t.db.testing.TestData.samples

  val liverParam = metadata.parameterSet.byId("liver_wt")
  val kidneyParam = metadata.parameterSet.byId("kidney_total_wt")

  test("basic") {
    for (s <- samples) {
      val cg = TestData.controlGroups(s)
      cg.controlSamples.size should equal(3)

      val remove = Set("dose_level", "individual_id")

      for (cs <- cg.controlSamples) {
        cs.sampleClass.constraints.filter(c => ! remove.contains(c._1)) should
          equal(s.sampleClass.constraints.filter(c => ! remove.contains(c._1)))
      }
    }
  }

  test("liver weight") {
    for (s <- samples;
      cg = TestData.controlGroups(s)) {
      val isControl = cg.controlSamples.toSet.contains(s)

      if (!isControl) {
        metadata.parameter(s, "liver_wt").get.toDouble should
          (be >= 4.9 and be <= 5.1)
      } else {
        metadata.parameter(s, "liver_wt").get.toDouble should
          (be >= 2.9 and be <= 3.1)
      }

      val time = metadata.parameter(s, "exposure_time").get
      println(cg.allParamVals(time))

      //TODO might need to adjust these limits
      cg.lowerBound(liverParam, time).get should
        (be >= 2.7 and be <= 3.1)
      cg.upperBound(liverParam, time).get should
        (be >= 2.9 and be <= 3.3)
    }
  }
}
