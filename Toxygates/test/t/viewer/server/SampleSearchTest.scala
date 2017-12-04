package t.viewer.server

import scala.collection.JavaConversions._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import otg.model.sample.OTGAttribute._
import otg.testing.TestData
import otgviewer.shared.OTGSchema
import t.TTestSuite
import t.common.server.sample.search.IndividualSearch
import t.common.server.sample.search.UnitSearch
import t.common.shared.sample.search.AndMatch
import t.common.shared.sample.search.AtomicMatch
import t.common.shared.sample.search.MatchCondition
import t.common.shared.sample.search.MatchType
import t.common.shared.sample.search.OrMatch
import t.model.sample.Attribute
import t.viewer.server.Conversions._

@RunWith(classOf[JUnitRunner])
class SampleSearchTest extends TTestSuite {
  def atomic(attribute: Attribute, mt: MatchType) =
    new AtomicMatch(attribute, mt, null)

  def atomic(attribute: Attribute, mt: MatchType, param: Double) =
    new AtomicMatch(attribute, mt, param)

  def or(mc1: MatchCondition, mc2: MatchCondition) =
    new OrMatch(Seq(mc1, mc2))

  def and(mc1: MatchCondition, mc2: MatchCondition) =
    new AndMatch(Seq(mc1, mc2))

  val schema = new OTGSchema
  val samples = t.db.testing.TestData.samples.toSeq.map(asJavaSample)

  val attributes = TestData.attribSet

  def sampleSearch(cond: MatchCondition) = {
    val searchParams = cond.neededParameters()
    IndividualSearch(samples, cond, new UnitsHelper(schema), attributes).results
  }

  def unitSearch(cond: MatchCondition) = {
    val searchParams = cond.neededParameters()
    UnitSearch(samples, cond, new UnitsHelper(schema), attributes).results
  }

  val atomicCondition = atomic(LiverWeight, MatchType.High)

  test("atomic-sample") {
    val r = sampleSearch(atomicCondition)
    for (s <- r) {
      s.get(Individual) should (equal ("1") or (equal ("3")))
    }
    // 4/5 of the samples are treated samples, and 2/3 of those treated samples have
    // ID 1 or 3 and hence high liver weight
    r.size should equal(2 * samples.size / 3 * 4 / 5)
  }

  test("atomic-unit") {
    val result = unitSearch(atomicCondition)
    result.size should equal(samples.size / 3 * 4 / 5)
  }

  val normalCondition = atomic(KidneyWeight, MatchType.NormalRange)

  test("normal-sample") {
    val result = sampleSearch(normalCondition)
    // All Low and Really high dose samples (2/5), 1/3 of High dose samples (1/15),
    //
    result.size should equal(samples.size * 7 / 15)
  }

  test("normal-unit") {
    val result = unitSearch(normalCondition)
    // High and Low dose units (2/5); 3 samples per unit
    //
    result.size should equal(samples.size / 3 * 2 / 5)
  }

  test("and") {
    val r = sampleSearch(and(
        atomic(LiverWeight, MatchType.High),
        atomic(KidneyWeight, MatchType.Low)))
    // Dose High Individual 3 (1/5 * 1/3) + Dose Middle Individual 1 or 3 (1/5 * 2/3)
    r.size should equal(samples.size / 5)
  }

  test("or") {
     val r = sampleSearch(or(
         atomic(LiverWeight, MatchType.High),
         atomic(KidneyWeight, MatchType.Low)))
    // Dose Middle (1/5) + Dose Low/High/Really High && Individual 1 or 3 (3/5 * 2/3)
    r.size should equal(samples.size * 3 / 5 )
  }

  test("normal") {
    val result = sampleSearch(atomic(LiverWeight, MatchType.NormalRange))
    for (sample <- result) {
      sample.get(DoseLevel) should not equal ("Control")
    }
    result.size should equal(samples.size / 3 * 4 / 5)
  }

  test("belowLimit") {
    val result = sampleSearch(atomic(LiverWeight, MatchType.BelowLimit, 4))
    for (sample <- result) {
      sample.get(DoseLevel) should not equal ("Control")
    }
    result.size should equal(samples.size / 3 * 4 / 5)
  }

  test("aboveLimit") {
    val result = sampleSearch(atomic(LiverWeight, MatchType.AboveLimit, 4))
    result.size should equal(2 * samples.size / 3 * 4 / 5)
  }
}
