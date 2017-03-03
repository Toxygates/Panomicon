package t.viewer.server

import t.common.shared.sample.search._
import t.common.shared.sample.Sample

import scala.collection.JavaConversions._
import t.platform.ControlGroup
import t.common.shared.DataSchema

class SampleSearch(schema: DataSchema, condition: MatchCondition,
    samples: Iterable[Sample]) {
  val neededParams = condition.neededParameters()

  //Gather all the parameters for all samples.

  val controlGroups: Map[Sample, ControlGroup] = ???

  //Evaluate the search.
  lazy val results: Set[Sample] = results(condition)

  private def sampleParamValue(s: Sample, param: String): Option[Double] = ???

  def timePoints: Iterable[String] = ???
  def time(s: Sample): String = ???

  private def paramIsHigh(s: Sample, param: String): Boolean = {
    val pv = sampleParamValue(s, param)
    val ub = controlGroups(s).upperBound(param, time(s))
    (pv, ub) match {
      case (Some(p), Some(u)) => p > u
      case _ => false
    }
  }

  private def paramIsLow(s: Sample, param: String): Boolean = {
    val pv = sampleParamValue(s, param)
    val lb = controlGroups(s).lowerBound(param, time(s))
    (pv, lb) match {
      case (Some(p), Some(l)) => p < l
      case _ => false
    }
  }

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
    samples.filter(matches(_, condition.matchType, condition.paramId,
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
