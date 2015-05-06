package t.common.shared

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class SampleClassTest extends FunSuite {

  val testMap = Map("x" -> "x", 
      "y" -> "y", 
      "z" -> "z")
  
  def scWith(m: Map[String, String]) =
    new SampleClass(mapAsJavaMap(m))
  
  val testSc = scWith(testMap)
  val small = scWith(Map("x" -> "x"))
  val big = scWith(testMap + ("a" -> "a"))
  val unrel = scWith(Map("b" -> "b"))
  val incomp = scWith(Map("x" -> "y"))
  
  test("equality") {    
    assert(scWith(testMap) == testSc)
    assert(testSc != small)
    assert(testSc != big)  
  }
  
  test("compatible") {    
    assert(testSc.compatible(big))
    assert(big.compatible(testSc))
    assert(testSc.compatible(small))
    assert(small.compatible(testSc))
    assert(testSc.compatible(testSc))
    assert(testSc.compatible(unrel))
    assert(unrel.compatible(testSc))    
    assert(!testSc.compatible(incomp))
    assert(!incomp.compatible(testSc))
  }
  
  test("collect") {
    assert(SampleClass.collect(seqAsJavaList(List(testSc, incomp)), "x").toSet
        == Set("x", "y"))
  }
}