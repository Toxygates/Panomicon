package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import otgviewer.shared.RuleType
import otgviewer.shared.RankRule
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class SeriesServiceTest extends FunSuite with BeforeAndAfter {

  var s: SeriesServiceImpl = _
  
  before {   
    val conf = new Configuration("otg", "/ext/toxygates")
    s = new SeriesServiceImpl()
    s.localInit(conf)
  }
  
  after {
	s.destroy
  }
  
  test("Ranking") {
    val f = SparqlServiceTest.testFilter
    val r = new RankRule(RuleType.MaximalFold, "1370365_at") //GSS gene
    
    val res = s.rankedCompounds(f, Array(r)).toSeq
    println(res take 10)
  }
  
}