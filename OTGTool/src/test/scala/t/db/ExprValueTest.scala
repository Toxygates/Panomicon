package t.db

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.TTestSuite

@RunWith(classOf[JUnitRunner])
class ExprValueTest extends TTestSuite {
  test("equality") {
    ExprValue(0, 'A') should equal(ExprValue(0, 'A'))
  }
}
