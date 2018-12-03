package t.common.client

import org.scalatest._
import otg.model.sample.AttributeSet
import t.model.SampleClass
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import otg.model.sample.OTGAttribute

@RunWith(classOf[JUnitRunner])
class UtilsTest extends FunSuite with Matchers {
  val attributes = AttributeSet.getDefault

  test("unpackSampleClass gracefully handles an odd number of input tokens") {
    val input = "test_type,,,SAT,,,sin_rep_type"
    var sampleClass = Utils.unpackSampleClass(attributes, input)

    sampleClass shouldBe a [SampleClass]
    // We should end up with two keys: test_type, which was in the input, and
    // type, which is added by unpackSampleClass. sin_rep_type, which didn't have
    // a value, should not be in there.
    sampleClass.getKeys.size shouldBe 2
    sampleClass.getKeys.contains(OTGAttribute.Repeat) shouldBe false
    sampleClass.get(OTGAttribute.TestType) shouldBe "SAT"
  }
}
