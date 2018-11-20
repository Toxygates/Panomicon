package t.platform.mirna

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.TestData
import t.platform._
import t.db.testing.NetworkTestData

@RunWith(classOf[JUnitRunner])
class TargetTableTest extends TTestSuite {
   test("basic") {
     val pf1 = TestData.platform(100, "p1-")
     val pf2 = TestData.platform(100, "p2-")
     
     val tt = NetworkTestData.targetTable(pf1, pf2, 100, 0.5)
     val assocs = tt.toVector
     
     assert(tt === assocs)
     assert(tt.scoreFilter(90).toSet === assocs.filter(_._3 >= 90).toSet)
     assert(tt.scoreFilter(0).toSet === tt.toSet)
   }
}