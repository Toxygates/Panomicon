package t.viewer.shared

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringListTest extends FunSuite {
  
  val items = List("a", "b", "c")
  
  test("basic") {
    val l = new StringList("probes", "test.name", items.toArray)
    assert(l.size() === items.size)
    assert(l.items() === items.toArray)
    val p = l.pack()
    val up = ItemList.unpack(p)
    assert(up.packedItems().toArray === items.toArray)
    assert(up.name() === l.name())
    assert(up.`type` === l.`type`)
  }
}