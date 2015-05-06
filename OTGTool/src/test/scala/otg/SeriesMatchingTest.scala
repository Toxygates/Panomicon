package otg

import org.junit.runner.RunWith
import t.SeriesRanking.safePCorrelation
import friedrich.data.Statistics.pearsonCorrelation
import org.scalatest.junit.JUnitRunner

//TODO move to package t

@RunWith(classOf[JUnitRunner])
class SeriesMatchingTest extends OTGTestSuite {
//	import SeriesRanking._
//	import friedrich.data.Statistics._
//	
//	test("Pearson correlation with missing values") {
//	  val d1 = Series(null, null, null, 0, null, null, 
//	      data = Vector(ExprValue(1.0), ExprValue(2.0, 'A'), ExprValue(3.0)))
//	  val d2 = Series(null, null, null, 0, null, null, 
//	      data = Vector(ExprValue(3.0), ExprValue(4.0), ExprValue(5.0)))
//	  
//	  safePCorrelation(d1, d2) should equal(pearsonCorrelation(Seq(0.0, 1.0, 3.0), Seq(0.0, 3.0, 5.0)))
//	}
//	
//	test("Pearson correlation with insufficient values") {
//	  //Only one mutual present value
//	  val d1 = Series(null, null, null, 0, null, null, 
//	      data = Vector(ExprValue(1.0), ExprValue(2.0, 'A'), ExprValue(3.0)))
//	  val d2 = Series(null, null, null, 0, null, null, 
//	      data = Vector(ExprValue(3.0, 'A'), ExprValue(4.0), ExprValue(5.0)))
//	  
//	  val x = safePCorrelation(d1, d2)
//	  assert(java.lang.Double.isNaN(x))
//	}
}