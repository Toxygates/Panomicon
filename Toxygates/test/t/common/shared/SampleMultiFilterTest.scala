package t.common.shared

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._


@RunWith(classOf[JUnitRunner])
class SampleMultiFilterTest extends FunSuite {

  def jset(x: String) = {
    val r = new java.util.HashSet[String]
    r.add(x)
    r
  }
  
  def jmap[T,U](x: Map[T, U]): java.util.Map[T, U] = 
    new java.util.HashMap(mapAsJavaMap(x))
  
  test("Empty") {
    val smf = new SampleMultiFilter(jmap(Map()))
    val sc = new SampleClass(jmap(Map("a" -> "x")))
    assert(smf.accepts(sc))
  }
  
  test("Accept") {
    val smf = new SampleMultiFilter(jmap(Map("a" -> jset("x"))))
    smf.addPermitted("a", "y")
    val sc = new SampleClass(jmap(Map("a" -> "x")))
    assert(smf.accepts(sc))
  }
  
  test("Fail 1") {
    val smf = new SampleMultiFilter(jmap(Map()))
    smf.addPermitted("a", "y")
    val sc = new SampleClass(jmap(Map("a" -> "x")))
    assert(!smf.accepts(sc))
  }

  test("Fail 2") {
    val smf = new SampleMultiFilter(jmap(Map()))
    smf.addPermitted("a", "y")
    val sc = new SampleClass(jmap(Map("b" -> "y")))
    assert(!smf.accepts(sc))
  }
}