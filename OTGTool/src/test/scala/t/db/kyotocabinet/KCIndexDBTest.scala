package t.db.kyotocabinet

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.TestData

@RunWith(classOf[JUnitRunner])
class KCIndexDBTest extends TTestSuite {
  import KCDBTest._
  import TestData._

  test("basic") {
    val db = memDBHash
    val kci = new KCIndexDB(memDBHash)
    for (k <- probeMap.tokens) {
      kci.put(k)
    }
    var m = kci.fullMap
    m.keys.toSet should equal(probeMap.tokens)
    m.values.toSet should equal ((0 until probeMap.data.size).toSet)

    val remove = TestData.probeMap.tokens.take(5)
    val removed = probeMap.tokens -- remove
    val removedKeys = probeMap.keys -- remove.map(m)

    for (k <- remove) {
      kci.remove(k)
    }
    m = kci.fullMap
    m.keys.toSet should equal(removed)
    m.values.toSet should equal(removedKeys)

    kci.release
  }

  test("named enum") {
    //TODO
  }

}
