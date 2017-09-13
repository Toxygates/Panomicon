package t.viewer.server

import scala.collection.JavaConversions._
import otgviewer.shared.OTGSchema
import t.TTestSuite
import t.common.shared.sample.Sample
import t.common.shared.sample.search.AndMatch
import t.common.shared.sample.search.AtomicMatch
import t.common.shared.sample.search.MatchCondition
import t.common.shared.sample.search.MatchType
import t.common.shared.sample.search.OrMatch
import t.common.server.sample.search.IndividualSearch
import t.db.VarianceSet
import otg.testing.TestData
import t.viewer.server.Conversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.common.shared.sample.StringBioParamValue
import otg.model.sample.OTGAttribute._
import t.common.server.sample.search.AbstractSampleSearch
import t.model.sample.BasicAttribute
import t.model.sample.Attribute

@RunWith(classOf[JUnitRunner])
class SampleSearchTest extends TTestSuite {
  def atomic(attribute: Attribute, mt: MatchType) =
    new AtomicMatch(attribute, mt, null)

  def or(mc1: MatchCondition, mc2: MatchCondition) =
    new OrMatch(Seq(mc1, mc2))

  def and(mc1: MatchCondition, mc2: MatchCondition) =
    new AndMatch(Seq(mc1, mc2))

  val schema = new OTGSchema
  val samples = t.db.testing.TestData.samples.toSeq.map(asJavaSample)

  val attributes = TestData.attribSet

  def search(cond: MatchCondition) = {
    val searchParams = cond.neededParameters()
    val ss: IndividualSearch =
      IndividualSearch(samples, cond, new UnitsHelper(schema), attributes)
    ss.results
  }

  test("atomic") {
    val r = search(atomic(LiverWeight, MatchType.High))
    for (s <- r) {
      s.get(Individual.id) should (equal ("1") or (equal ("3")))
    }
    // 4/5 dose levels, individuals 1, 3
    r.size should equal(2 * samples.size / 3 * 4 / 5)
  }

  test("and") {
    val r = search(
        and(
            atomic(LiverWeight, MatchType.High),
            atomic(KidneyWeight, MatchType.Low)
            )
        )
    for (s <- r) {
      s.get(Individual.id) should equal("3")
    }
    r.size should equal(samples.size / 3 * 4 / 5)
  }

  test("or") {
     val r = search(
        or(
            atomic(LiverWeight, MatchType.High),
            atomic(KidneyWeight, MatchType.Low)
            )
         )
    r.size should equal(3 * samples.size / 3 * 4 / 5)
  }

}
