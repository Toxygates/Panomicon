package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import otg.ExprValue

@RunWith(classOf[JUnitRunner])
class ExprMatrixTest extends FunSuite {

  def testMatrix = {
    val data = List(
      List(3, 3, 5, 3, 3, 5),
      List(1, 2, 1, 9, 8, 10),
      List(2, 1, 1, 19, 18, 20),
      List(4, 4, 4, 2, 1, 2),
      List(5, 2, 3, 2, 4, 3)).map(_.map(ExprValue(_)))
    val em = ExprMatrix.withRows(data)
    em.columnMap = Map("a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3, "e" -> 4, "f" -> 5)
    em.rowMap = Map("a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3, "e" -> 4)
    em
  }
  
  /**
   * Test data
   *
   * 3	3	5	3	3	5	0.422649731
   * 1	2	1	9	8	10	0.008391321
   * 2	1	1	19	18	20	0.001609947
   * 4	4	4	2	1	2	0.125665916
   * 5	2	3	2	4	3	0.822206065
   *
   * First 3 columns are group 1, following 3 group 2, last column expected p-value
   *
   */
  test("t-test and sorting") {    
    val em = testMatrix
    assert(em.columns === 6)
    assert(em.rows === 5)
    
    val em2 = em.appendTTest(em, Seq("a", "b", "c"), Seq("d", "e", "f"))
    assert(em2.columns == 7)
    
    val em3 = em2.sortRows((v1, v2) => v1(6).value < v2(6).value)
    println(em3.row(0))
    assert(em3(0,6).value < 0.002)    
    assert(em3(4,6).value > 0.8)
    
    val em4 = em3.sortRows((v1, v2) => v1(6).value > v2(6).value)
    println(em4.row(0))
    assert(em4(4,6).value < 0.002)    
    assert(em4(0,6).value > 0.8)
    
  }
  
  test("sorting") {
    val em = testMatrix
    val em2 = em.sortRows((v1, v2) => v1(0).value < v2(0).value)
    println(em2)
    println(em2.rowMap)
    assert(em2.rowMap("b") == 0)
    
    val em3 = em2.sortRows((v1, v2) => v1(0).value > v2(0).value)
    println(em3)
    println(em3.rowMap)
    
    assert(em2.toRowVectors.reverse === em3.toRowVectors)
  }
  
  test("row and column select") {
    val em = testMatrix
    val em1 = em.selectRows(List(1,3,4))
    assert(em1.rows == 3)
    assert(em1.rowMap == Map("b" -> 1, "d" -> 3, "e" -> 4))
    assert(em1.columnMap == em.columnMap)
    val em2 = em1.selectColumns(List(1,3,4))
    assert(em2.columns == 3)
    assert(em2.rows == 3)
    assert(em2.rowMap == em1.rowMap)
    assert(em2.columnMap == Map("b" -> 1, "d" -> 3, "e" -> 4))
    
  }

}