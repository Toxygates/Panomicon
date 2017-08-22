package t.viewer.server

import scala.collection.JavaConversions.seqAsJavaList

import otgviewer.shared.OTGSchema
import t.TTestSuite
import t.common.shared.sample.Sample
import t.common.shared.sample.search.AndMatch
import t.common.shared.sample.search.AtomicMatch
import t.common.shared.sample.search.MatchCondition
import t.common.shared.sample.search.MatchType
import t.common.shared.sample.search.OrMatch
import t.common.server.sample.search.IndividualSearch
import t.platform.VarianceSet
import otg.testing.TestData
import t.viewer.server.Conversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.common.shared.sample.StringBioParamValue
import otg.model.sample.Attribute._
import t.common.server.sample.search.AbstractSampleSearch
import t.model.sample.BasicAttribute

@RunWith(classOf[JUnitRunner])
class SampleSearchTest extends TTestSuite {
  def atomic(param: String, mt: MatchType) =
    new AtomicMatch(
        new BasicAttribute(param, param),
        mt, null)

  def or(mc1: MatchCondition, mc2: MatchCondition) =
    new OrMatch(Seq(mc1, mc2))

  def and(mc1: MatchCondition, mc2: MatchCondition) =
    new AndMatch(Seq(mc1, mc2))

  val schema = new OTGSchema
  val cgroups = TestData.controlGroups.map(x =>
    asJavaSample(x._1) -> x._2)
  val samples = t.db.testing.TestData.samples

  val attributes = TestData.attribSet

  def search(cond: MatchCondition) = {
    val searchParams = IndividualSearch.conditionParams(attributes,
        cond)
    val ss: IndividualSearch = new IndividualSearch(TestData.metadata,
        cond, cgroups, samples.toSeq.map(asJavaSample), searchParams)
    ss.results
  }

  test("atomic") {
    val r = search(atomic(LiverWeight.title, MatchType.High))
    for (s <- r) {
      println(s.sampleClass())
      val cg = TestData.controlGroups(asScalaSample(s))
      println(cg.upperBound(LiverWeight, 1))

      println(cg.samples.map(TestData.metadata.parameter(_, LiverWeight)))
      s.get(Individual.id) should (equal ("1") or (equal ("3")))
    }
    // 4/5 dose levels, individuals 1, 3
    r.size should equal(2 * samples.size / 3 * 4 / 5)
  }

  test("and") {
    val r = search(
        and(
            atomic(LiverWeight.title, MatchType.High),
            atomic(KidneyWeight.title, MatchType.Low)
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
            atomic(LiverWeight.title, MatchType.High),
            atomic(KidneyWeight.title, MatchType.Low)
            )
         )
    r.size should equal(3 * samples.size / 3 * 4 / 5)
  }

}
