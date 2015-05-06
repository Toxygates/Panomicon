package otgviewer.server.rpc

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import t.viewer.server.Configuration
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
    val sc = SparqlServiceTest.testSampleClass
    val r = new RankRule(RuleType.MaximalFold, "1370365_at") //GSS gene
    
    //TODO needs a valid dataset for the first argument
    val res = s.rankedCompounds(Array(), sc, Array(r)).toSeq
    println(res take 10)
  }
  
}