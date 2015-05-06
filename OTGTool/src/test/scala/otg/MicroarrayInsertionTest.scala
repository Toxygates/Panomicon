package otg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.MemoryLookupMap
import t.db.testing.FakeBasicMatrixDB
import t.db.testing.FakeRawExpressionData
import t.testing.FakeContext
import t.db.SampleIndex
import t.db.Sample
import t.db.ProbeMap

@RunWith(classOf[JUnitRunner])
class OTGInsertTest extends OTGTestSuite {
  
  val ids = (1 to 24).toStream.iterator
  val samples = for (dose <- Set("Control", "High"); time <- Set("3hr", "24hr");
    ind <- Set("1", "2", "3"); compound <- Set("Chocolate", "Tea");
    s = Sample("s" + ids.next, Map("dose_level" -> dose, "individual_id" -> ind, 
        "exposure_time" -> time, "compound_name" -> compound))
  ) yield s
		  
  def randomExpr(): (Double, Char, Double) = {
    val v = Math.random * 10000
    val call = (Math.random * 3).toInt match {
      case 0 => 'A'
      case 1 => 'P'
      case 2 => 'M'
    } 
    (v, call, Double.NaN)
  }

  val probes = (1 to 10)
  implicit val probeMap = {
    val pmap = Map() ++ probes.map(x => ("probe_" + x -> x))
    new MemoryLookupMap(pmap) with ProbeMap
  }

  val dbIdMap = { 
    val dbIds = Map() ++ samples.zipWithIndex.map(s => (s._1.sampleId -> s._2))
    new SampleIndex(dbIds)
  }
  
  implicit val context = new FakeContext(dbIdMap, probeMap)

  def makeTestData(): FakeRawExpressionData = {
    var testData = Map[Sample, Map[String, (Double, Char, Double)]]()
    for (s <- samples) {
      var thisProbe = Map[String, (Double, Char, Double)]()
      for (p <- probeMap.tokens) {
        thisProbe += (p -> randomExpr())
      }
      testData += (s -> thisProbe)
    }
    new FakeRawExpressionData(testData)
  }

  test("Absolute value insertion") {
    val data = makeTestData()
    val db = new FakeBasicMatrixDB()
    val builder = new AbsoluteValueBuilder()
    val inserter = new MicroarrayInsertion(() => db)
    inserter.insertFrom("Absolute value data insert", data, builder)
    val s1 = db.records.map(ev => (ev._1, ev._2, ev._3.value, ev._3.call)).toSet
    val s2 = data.data.flatMap(x => x._2.map(y => (x._1, y._1, y._2._1, y._2._2))).toSet
    s1 should equal(s2)    
  }

//  test("Folds") {
//    val data = makeTestData()
//    val db = new FakeMicroarrayDB()
//    val builder = new BasicFoldValueBuilder()
//  }
}