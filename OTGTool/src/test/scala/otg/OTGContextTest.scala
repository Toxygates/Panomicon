package otg

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import otg.Species._
import t.testing.TestConfig

@RunWith(classOf[JUnitRunner])
class OTGContextTest extends OTGTestSuite {

  val config = TestConfig.config
  val context = new OTGContext(config)
  
  test("probe map") {
    val m1 = context.probeMap
    
    println(m1.tokens.size)
    
    
    assert(m1.tokens.size === m1.keys.size)
    
    val probes1 = List(0, 1500, 2382, 30000)    

    println(m1.keys)
    
    for (p <- probes1) {
      val str = m1.unpack(p)
      assert(m1.pack(str) === p)
    }    
  }  
}