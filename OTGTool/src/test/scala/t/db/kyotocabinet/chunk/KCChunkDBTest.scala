package t.db.kyotocabinet.chunk

import org.junit.runner.RunWith
import t.TTestSuite
import org.scalatest.junit.JUnitRunner
import t.db.kyotocabinet.KCDBTest
import t.db.testing.TestData
import t.db.PExprValue

@RunWith(classOf[JUnitRunner])
class KCChunkMatrixDBTest extends TTestSuite {
  import KCDBTest._
  import TestData._
  import KCChunkMatrixDB._

  test("basic") {
    val db = memDBHash
    val edb = new KCChunkMatrixDB(db)

    testExtDb(edb, makeTestData(false))
    edb.release
  }

  test("sparse data") {
    val db = memDBHash
    val edb = new KCChunkMatrixDB(db)

    testExtDb(edb, makeTestData(true))
    edb.release
  }

  test("Vector Chunk") {
    def mkValues(n: Int) = (0 until n).map(i => randomPExpr(probeMap.unpack(i)))

    var vc = new VectorChunk[PExprValue](0, 0, Seq())
    val xs = (mkValues(500).zipWithIndex)

    val p1 = xs.take(100)
    for (x <- p1) {
      vc = vc.insert(x._2, x._1)
    }
    vc.xs.size should equal(100)
    vc.probes should equal((0 until 100))

    vc = vc.remove(49)
    vc = vc.remove(19)
    vc.xs.size should equal(98)
    val valid = (p1.filter(x => x._2 != 49 && x._2 != 19))
    vc.xs.map(x => (x._2, x._1)) should equal(valid)

    vc = vc.insert(xs(10)._2, xs(10)._1)
    vc = vc.insert(xs(30)._2, xs(30)._1)
    vc.xs.map(x => (x._2, x._1)) should equal(valid)

  }
}
