package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import t.common.shared.sample.ExpressionValue

@RunWith(classOf[JUnitRunner])
class EVArrayTest extends FunSuite {

  test("basic") {
    val d = (1 to 3).map(x => new ExpressionValue(x, 'M', "tooltip"))
    val a = EVArray(d)
    assert(a === d)
    assert(a.length === 3)
    assert(a(0) === d(0))    
    
    val x = new ExpressionValue(4, 'M', "tooltip")
    val big = a :+ x
    assert(a === d)
    assert(big === d :+ x)
    assert(big.length == 4)
    
    val b2 = EVArray(big)
    assert(b2 === big)
  }
  
  implicit def builder() = EVABuilder()
  
  test("append") {
    val d = (1 to 4).map(x => new ExpressionValue(x, 'M', "tooltip"))
    
    val b1 = EVArray(d take 2)
    val b2 = EVArray(d drop 2)    
    val app = b1 ++ b2
    println(app)
    assert(app === d)
  }
  
  test("builder") {
    val d = (1 to 3).map(x => new ExpressionValue(x, 'M', "tooltip"))
    val ea = (EVABuilder() ++= d).result
    assert(ea === d)
    
    var b = EVABuilder()
    for (i <- d) {
      b += i      
    }
        
    var b2 = EVABuilder()
    b2 ++= d
    assert(b.result === b2.result)
  }
}