package otg

import org.junit.runner.RunWith 
import otg.RepeatType.Repeat
import otg.RepeatType.RepeatType
import otg.RepeatType.Single
import t.db.SeriesDB
import t.db.kyotocabinet.KCSeriesDB
import org.scalatest.junit.JUnitRunner
import otg.Organ._
import otg.Species._
import t.testing.TestConfig

@RunWith(classOf[JUnitRunner])
class SeriesTest extends OTGTestSuite {
//  var db: SeriesDB = _

  val config = TestConfig.config
  //TODO change
  implicit val context = new OTGContext(config)
  
//  before {
//    // TODO change the way this is configured
////    System.setProperty("otg.home", "/Users/johan/otg/20120221/open-tggates")
//    db = new KCSeriesDB(System.getProperty("otg.home") + "/otgfs.kct")(context)
//  }
//
//  test("Series retrieval") {
//    val cs = context.compounds
//    val packed = context.unifiedProbes.pack("1393108_at")
//    for (c <- cs.tokens) {
//      val key = new Series(Single, Liver, Rat, packed, c, null, null)
//      val ss = db.read(key)
//      if (!ss.isEmpty) {
//        val s = ss.head
//        println(s)
//        s.compound should equal(c)
//        s.probe should equal(packed)
//      }
//    }
//  }
//
//  test("Series retrieval with blank compound") {
//    val cs = context.compounds
//    val packed = context.unifiedProbes.pack("1393108_at")
//    def checkCombination(r: RepeatType, o: Organ, s: Species, expectedCount: Int) {      
//      val key = new Series(r, o, s, packed, null, null, null)
//      val ss = db.read(key)
//      val n = ss.map(_.compound).toSet.size
//      println(s"$r/$o/$s: $n")
//      assert(n === expectedCount)
//    }
//    
//    // These counts are valid for otg-test (adjuvant version)
//    checkCombination(Single, Liver, Rat, 162)
//    checkCombination(Repeat, Liver, Rat, 143)
//    checkCombination(Single, Kidney, Rat, 43)
//    checkCombination(Repeat, Kidney, Rat, 41)
//    checkCombination(Single, Vitro, Rat, 145)
//    checkCombination(Single, Vitro, Human, 158)
//  }
//  
//  test("Fill empty series for insertion") {
//    import OTGSeriesInsert._
//    val testCases = List(
//        (Vitro, Single, List("2 hr", "8 hr"), true, "Middle"),
//        (Liver, Single, List("6 hr", "24 hr"), true, "Low"),
//        (Liver, Repeat, List("4 day", "29 day"), true, "High"),
//        (Liver, Single, List("Middle"), false, "3 hr")
//        )
//    val expected = List(
//        List("2 hr", "8 hr", "24 hr"),
//        List("3 hr", "6 hr", "9 hr", "24 hr"),
//        List("4 day", "8 day", "15 day", "29 day"),
//        List("Low", "Middle", "High")
//        )
//    for ((t,e) <- testCases zip expected) {
//      val f = Filter(Some(t._1), Some(t._2), Some(Rat))
//      val points = t._4 match {
//        case true => t._3.map(SeriesPoint(_, t._5, ExprValue(0, 'A')))
//        case false => t._3.map(SeriesPoint(t._5, _, ExprValue(0, 'A')))
//      }
//      val filled = fillSeriesForInsertion(f, points, t._4, t._5)
//      if (t._4) {
//        assert(filled.toList.map(_.time) === e)
//      } else {
//        assert(filled.toList.map(_.dose) === e)
//      }
//    }        
//  }
  
//  after {
//    db.close()
//  }
}