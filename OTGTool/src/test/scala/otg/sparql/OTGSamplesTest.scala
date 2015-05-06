package otg.sparql

import scala.Option.option2Iterable
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import otg.OTGContext
import otg.Organ.Kidney
import otg.Organ.Liver
import otg.RepeatType.Repeat
import otg.RepeatType.Single
import otg.Species.Rat
import t.testing.TestConfig
import org.scalatest.junit.JUnitRunner
import t.sparql._

@RunWith(classOf[JUnitRunner])
class OTGSamplesTest extends FunSuite with BeforeAndAfter with ShouldMatchers {   

  val config = TestConfig.config
  implicit val context = new OTGContext(config)
  val samples = new OTGSamples(config) 
      
  after {
    samples.close
  }
  
  //TODO update tests

//  test("organs") {
//    val f = Filter(Some(Kidney), Some(Repeat), Some(Rat), Some("in vivo"))
//    val os = samples.organs(f, None)
//    //NA organs are for those samples that have no arrays
//    assert(os.toSet === Set("Liver", "Kidney"))
//  }
//
//  test("dose levels") {
//    val f = Filter(Some(Liver), Some(Single), Some(Rat), Some("in vivo"))
//    val ds = samples.doseLevels(f, None)
//    //TODO use the unified DataSchema instead
////    assert(ds.toSet === TimeDose.doses.toSet)
//  }
//
//  test("times") {
//    val f = Filter(Some(Liver), Some(Single), Some(Rat), Some("in vivo"))
//    val ts = samples.times(f, None)
//   //TODO use the unified DataSchema instead
////    assert(ts.toSet.subsetOf(TimeDose.times.toSet))
//  }
//
//  test("times and doses") {
//    val f = Filter(Some(Liver), Some(Single), Some(Rat), Some("in vivo"))
//    val compound = "acetaminophen"
//    val td = samples.timeDoseCombinations(f)
//    assert(td.size === 16)
//    val s = td.toSet
//    assert(s.contains(("24 hr", "Control")))
//    assert(s.contains(("3 hr", "Middle")))
//    assert(s.contains(("9 hr", "High")))
//  }

//  test("barcodes") {
//    val bcs = samples.barcodes(Filter(Some(Liver), None, None), Some("adapin"), None, None)
//    bcs.size should equal(84)
//    println(bcs)
//  }

}