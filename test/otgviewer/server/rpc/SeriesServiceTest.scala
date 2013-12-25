package otgviewer.server.rpc

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import otgviewer.shared.RuleType
import otgviewer.shared.RankRule
import org.scalatest.BeforeAndAfter
import otgviewer.server.rpc.SeriesServiceImpl
import otgviewer.server.Configuration
import otgviewer.server.rpc.SparqlServiceTest
import org.scalatest.junit.JUnitRunner

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