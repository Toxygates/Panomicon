package t.global

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * @author johan
 */

@RunWith(classOf[JUnitRunner])
class KCDBRegistryTest extends TTestSuite {
  val testFile = "test.kct#bnum=1000"
  val testFileShort ="test.kct"

  test("Basic") {
    //creates the file
    val w = KCDBRegistry.getWriter(testFile)
    w should not equal(None)

    KCDBRegistry.isInWriting(testFile) should equal(true)
    KCDBRegistry.threadHasWriter(testFile) should equal(true)

    KCDBRegistry.getReadCount(testFile) should equal(0)
    val r = KCDBRegistry.getReader(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(1)
    assert (r.get eq w.get)

    KCDBRegistry.getReader(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(2)
    KCDBRegistry.getReadCount(testFileShort) should equal(2)

    KCDBRegistry.release(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(1)

    KCDBRegistry.release(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(0)
    KCDBRegistry.getReadCount(testFileShort) should equal(0)

    KCDBRegistry.release(testFile)
    KCDBRegistry.isInWriting(testFile) should equal(false)
    KCDBRegistry.threadHasWriter(testFile) should equal(false)
    KCDBRegistry.getReadCount(testFile) should equal(0)
  }

}
