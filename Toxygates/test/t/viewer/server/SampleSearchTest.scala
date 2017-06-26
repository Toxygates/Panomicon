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
import t.platform.ControlGroup
import otg.testing.TestData
import t.viewer.server.Conversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.SampleParameters._

@RunWith(classOf[JUnitRunner])
class SampleSearchTest extends TTestSuite {
  def atomic(param: String, mt: MatchType) =
    new AtomicMatch(param, mt, null)

  def or(mc1: MatchCondition, mc2: MatchCondition) =
    new OrMatch(seqAsJavaList(Seq(mc1, mc2)))

  def and(mc1: MatchCondition, mc2: MatchCondition) =
    new AndMatch(seqAsJavaList(Seq(mc1, mc2)))

  val schema = new OTGSchema
  val cgroups = TestData.controlGroups.map(x =>
    asJavaSample(x._1) -> x._2)
  val samples = t.db.testing.TestData.samples

  val paramSet = TestData.metadata.parameterSet

  def search(cond: MatchCondition) = {
    val searchParams = SampleSearch.conditionParams(paramSet,
        cond)
    val ss: SampleSearch[Sample] = SampleSearch.forSample(TestData.metadata,
        cond, schema, cgroups, searchParams, samples.toSeq.map(asJavaSample))
    ss.results
  }

  val _liverParam = paramSet.byId("liver_wt")
  val liverParam = _liverParam.humanReadable
  val _kidneyParam = paramSet.byId("kidney_total_wt")
  val kidneyParam = _kidneyParam.humanReadable

  test("atomic") {
    val r = search(atomic(liverParam, MatchType.High))
    for (s <- r) {
      println(s.sampleClass())
      val cg = TestData.controlGroups(asScalaSample(s))
      println(cg.upperBound(_liverParam, s.get(ExposureTime.id), 1))

      println(cg.controlSamples.map(TestData.metadata.parameter(_, _liverParam)))
      s.get(Individual.id) should (equal ("1") or (equal ("3")))
    }
    // 4/5 dose levels, individuals 1, 3
    r.size should equal(2 * samples.size / 3 * 4 / 5)
  }

  test("and") {
    val r = search(
        and(
            atomic(liverParam, MatchType.High),
            atomic(kidneyParam, MatchType.Low)
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
            atomic(liverParam, MatchType.High),
            atomic(kidneyParam, MatchType.Low)
            )
         )
    r.size should equal(3 * samples.size / 3 * 4 / 5)
  }

}
