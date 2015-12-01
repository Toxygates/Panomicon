package t.db.kyotocabinet

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.TTestSuite
import t.db.testing.TestData

@RunWith(classOf[JUnitRunner])
class KCExtMatrixDBTest extends TTestSuite {
  import KCDBTest._
  import TestData._

  test("basic") {
    val db = memDB
    val edb = new KCExtMatrixDB(db)

    testExtDb(edb, makeTestData(false))

    edb.release
  }

  test("sparse data") {
    val db = memDB
    val edb = new KCExtMatrixDB(db)

    testExtDb(edb, makeTestData(true))

    edb.release
  }
}
