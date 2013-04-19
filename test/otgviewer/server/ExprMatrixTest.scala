package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import otg.ExprValue
import bioweb.shared.array.ExpressionValue

@RunWith(classOf[JUnitRunner])
class ExprMatrixTest extends FunSuite {

  def testMatrix = {
    val data = List(
      List(3, 3, 5, 3, 3, 5),
      List(1, 2, 1, 9, 8, 10),
      List(2, 1, 1, 19, 18, 20),
      List(4, 4, 4, 2, 1, 2),
      List(5, 2, 3, 2, 4, 3)).map(_.map(new ExpressionValue(_)))
    val em = ExprMatrix.withRows(data)
    em.columnMap = Map("a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3, "e" -> 4, "f" -> 5)
    em.rowMap = Map("a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3, "e" -> 4)
    em.annotations = (1 to 5).map(x => ExprMatrix.RowAnnotation("p" + x, null, null, null)).toArray
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
    
    assert(em2("b", "b").value == 2)
    assert(em2("b", "c").value == 1)
    assert(em2("d", "a").value == 4)
    
    assert(em2.annotations(0).probe == "p2")
    assert(em2.annotations(1).probe == "p3")
    assert(em2.annotations(2).probe == "p1")
    
    val em3 = em2.sortRows((v1, v2) => v1(0).value > v2(0).value)
    println(em3)
    println(em3.rowMap)
    
    assert(em2.toRowVectors.reverse === em3.toRowVectors)
  }
  
  test("row and column select") {
    val em = testMatrix
    val em1 = em.selectRows(List(1,3,4))
    assert(em1.rows == 3)
    println(em1.rowMap)
    assert(em1.rowMap == Map("b" -> 0, "d" -> 1, "e" -> 2))
    assert(em1.columnMap == em.columnMap)
    assert(em1.annotations(0).probe == "p2")
    assert(em1.annotations(1).probe == "p4")
    assert(em1.annotations(2).probe == "p5")
    
    val em2 = em1.selectColumns(List(1,3,4))
    assert(em2.columns == 3)
    assert(em2.rows == 3)
    assert(em2.rowMap == em1.rowMap)
    assert(em2.columnMap == Map("b" -> 0, "d" -> 1, "e" -> 2))
    assert(em2.annotations(0).probe == "p2")
    assert(em2.annotations(1).probe == "p4")
    assert(em2.annotations(2).probe == "p5")
    
    //select and permute
    val em3 = em.selectRows(List(3,1,4))
    assert(em3.rows == 3)
    println(em3.rowMap)
    assert(em3.rowMap == Map("d" -> 0, "b" -> 1, "e" -> 2))
    assert(em3.columnMap == em.columnMap)
    assert(em3.annotations(0).probe == "p4")
    assert(em3.annotations(1).probe == "p2")
    assert(em3.annotations(2).probe == "p5")
    
    val em4 = em.selectNamedRows(List("e", "d"))
    assert(em4.rows == 2)
    println(em4.rowMap)
    assert(em4.rowMap == Map("e" -> 0, "d" -> 1))
    assert(em4.annotations(0).probe == "p5")
    assert(em4.annotations(1).probe == "p4")
  }
  
  test("filtering") {
    val em = testMatrix
    val f = em.filterRows(_.head.value > 2)
    assert(f.columnMap == em.columnMap)
    assert(f.rowMap.keySet subsetOf em.rowMap.keySet)
    assert(f.annotations(0) == em.annotations(0))
    assert(f.annotations(1) == em.annotations(3))
    assert(f.annotations(2) == em.annotations(4))
  }
  
  test("join and split") {
    val em = testMatrix
    val small = ExprMatrix.withRows(List(List(1),
        List(2),
        List(3),
        List(4),
        List(5)).map(_.map(new ExpressionValue(_))))
        
    val (s1, s2) = em.modifyJointly(small, _.sortRows((v1, v2) => v1(0).value < v2(0).value))
    println(em.rowMap)
    println(s1.rowMap)
    assert(s2.rowMap == s1.rowMap)
    assert(s2.annotations == s1.annotations)
    println(em.annotations.toVector)
    println(s2.annotations.toVector)
    println(s1.annotations.toVector)
    assert(s2.annotations(0).probe == "p2")
    assert(s2.annotations(1).probe == "p3")
    assert(s2.annotations(2).probe == "p1")
//    assert(em.rowMap == s2.rowMap)
    
  }

}