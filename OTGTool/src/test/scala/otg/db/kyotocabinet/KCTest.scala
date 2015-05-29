package otg.db.kyotocabinet

import org.junit.runner.RunWith
import t.db.ExprValue
import otg.OTGContext
import otg.OTGTestSuite
import t.db.MatrixDBReader
import org.scalatest.junit.JUnitRunner
import otg.Species._
import t.testing.TestConfig
import t.db.Sample

@RunWith(classOf[JUnitRunner])
class KCTest extends OTGTestSuite {
  var db: MatrixDBReader[ExprValue] = _
  
  val config = TestConfig.config
  implicit val context = new OTGContext(config)
  val pmap = context.probeMap
  
  before {    
	db = context.absoluteDBReader
  }
    
  after {
    db.release
  }
  
  test("Read data") {
    val s = Sample("003017629005")
    println(s.dbCode)
    println(pmap.keys.size + " probes known")
    println(pmap.keys.take(5))
    println(context.sampleMap.tokens.size + " samples known")
   
    println(context.sampleMap.tokens.take(5))
    val r = db.valuesInSample(s, List())
    r.size should equal(31042) //number of probes for rat arrays
  }
  
  test("Read by probe and barcode") {       
    // Verify that data read probe-wise equals data read barcode-wise
    val ss = db.sortSamples(List("003017629001", "003017629013", "003017629014").map(Sample(_)))
    val r1 = db.valuesInSamples(ss, List())
    r1.toVector.flatten.size should equal(93126)
    
    val ps = List("1371970_at", "1371034_at", "1372727_at")
    val packedProbes = ps.map(pmap.pack(_))
    val r1f = r1.map(_.filter(x => ps.contains(x.probe))).toSeq
    val pps = ps.zip(ps.map(pmap.pack(_)))
    val r2s = pps.map(x => db.valuesForProbe(x._2, ss))
    for (i <- 0 until ss.size) {
      val r1ForSample = r1f(i).toSet
      val r2ForSample = r2s.flatten.filter(_._1 == ss(i)).map(_._2).toSet      
      r1ForSample should equal(r2ForSample)
      println(r1ForSample + " == " + r2ForSample)
    }
  }
}