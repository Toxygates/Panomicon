package t.viewer.server

import t.common.shared.sample.search._
import t.common.shared.sample.Sample

import scala.collection.JavaConversions._

class SampleSearch(condition: MatchCondition,
    allSamples: Iterable[Sample]) {
  val neededParams = condition.neededParameters()

  //Gather all the parameters for all samples.

  //Evaluate the search.
  lazy val results: Set[Sample] = results(condition)

  private def sampleParamValue(s: Sample, param: String): Option[Double] = ???

  private def paramIsHigh(s: Sample, param: String): Boolean = ???

  private def paramIsLow(s: Sample, param: String): Boolean = ???

  private def results(condition: MatchCondition): Set[Sample] =
    condition match {
      case and: AndMatch =>
        and.subConditions.map(results _).reduce(_ intersect _)
      case or: OrMatch =>
        or.subConditions.map(results _).reduce(_ union _)
      case at: AtomicMatch =>
        results(at)
    }

  private def results(condition: AtomicMatch): Set[Sample] =
    allSamples.filter(matches(_, condition.matchType, condition.paramId,
      Option(condition.param1))).toSet

  private def matches(s: Sample, mt: MatchType, paramId: String,
    threshold: Option[Double]): Boolean =
    mt match {
      case MatchType.High => paramIsHigh(s, paramId)
      case MatchType.Low  => paramIsLow(s, paramId)
      case MatchType.NormalRange =>
        !paramIsHigh(s, paramId) && !paramIsLow(s, paramId)
      case _ =>
        sampleParamValue(s, paramId) match {
          case Some(v) =>
            mt match {
              case MatchType.AboveLimit => v >= threshold.get
              case MatchType.BelowLimit => v <= threshold.get
            }
          case None => false
        }
    }
}
