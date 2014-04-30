package otgviewer.server.rpc

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import otgviewer.server.Configuration
import otgviewer.shared.RankRule
import otgviewer.shared.RuleType
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SeriesServiceTest extends FunSuite with BeforeAndAfter {

  var s: SeriesServiceImpl = _
  
  before {   
    val conf = new Configuration("otg", "/shiba/toxygates", 2)
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