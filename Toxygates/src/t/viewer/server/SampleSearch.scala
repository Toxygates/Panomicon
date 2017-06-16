package t.viewer.server

import t.common.shared.sample.search._

import scala.collection.JavaConversions._
import t.platform.ControlGroup
import t.common.shared.DataSchema
import t.db.Metadata
import Conversions._
import t.common.shared.sample.Sample
import t.sparql.TriplestoreMetadata
import t.sparql.SampleFilter
import t.sparql.SampleClass
import t.sparql.CachingTriplestoreMetadata
import t.db.SampleParameter
import t.sparql.Samples
import t.sample.SampleSet
import t.db.ParameterSet

object SampleSearch {

  /**
   * Construct a SampleSearch instance.
   * @param condition the criteria to search for
   * @param samples the space of samples to search in
   * @param annotations source of information about the samples
   */
  def apply(data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[Sample])(implicit sf: SampleFilter): SampleSearch = {

    val schema = annotations.schema

    val sampleParams = annotations.baseConfig.sampleParameters

    val usedParams = conditionParams(sampleParams, condition)
    val coreParams = Seq("control_group", schema.timeParameter(), "sample_id").map(
        sampleParams.byId)
    val neededParams = (coreParams ++ usedParams).toSeq.distinct

    val metadata = new CachingTriplestoreMetadata(data, sampleParams, neededParams)
    val controlGroups = annotations.controlGroups(samples, metadata)
    new SampleSearch(annotations.schema, metadata, condition, controlGroups, samples,
        usedParams)
  }

  def conditionParams(paramSet: ParameterSet, cond: MatchCondition):
    Iterable[SampleParameter] = {
    val paramsByTitle = Map() ++
      paramSet.all.map(p => p.humanReadable -> p)
    cond.neededParameters().map(paramsByTitle)
  }
}

/**
 * Evaluates sample searches for a match condition.
 */
class SampleSearch(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, ControlGroup],
    samples: Iterable[Sample],
    searchParams: Iterable[SampleParameter]) {

  private val searchParamIds = searchParams.map(_.identifier)
  val humanReadableToParam = Map() ++ metadata.parameterSet.all.map(p =>
    p.humanReadable -> p)

  /**
   * Results of the search.
   */
  lazy val results: Iterable[Sample] =
    results(condition).toSeq.map(postMatchAdjust)

  /**
   * Insert additional parameter information in the sample (the parameters
   * that were used in the match condition).
   * The mutable sample class is modified in place.
   */
  private def postMatchAdjust(s: Sample): Sample = {
    val ss = asScalaSample(s)
    for (p <- searchParamIds;
      v <- metadata.parameter(ss, p)) {
      s.sampleClass().put(p, v)
    }
    s
  }

  //TODO
  private def sampleParamValue(s: Sample, param: SampleParameter): Option[Double] =
    try {
      metadata.parameter(asScalaSample(s), param.identifier) match {
        case Some("NA") => None
        case Some(s) => Some(s.toDouble)
        case None => None
      }
    } catch {
      case nf: NumberFormatException => None
    }

  def time(s: Sample): String = s.get(schema.timeParameter())

  private def paramIsHigh(s: Sample, param: SampleParameter): Boolean = {
    val pv = sampleParamValue(s, param)
    val ub = controlGroups.get(s).flatMap(_.upperBound(param, time(s)))
    (pv, ub) match {
      case (Some(p), Some(u)) => p > u
      case _ => false
    }
  }

  private def paramIsLow(s: Sample, param: SampleParameter): Boolean = {
    val pv = sampleParamValue(s, param)
    val lb = controlGroups.get(s).flatMap(_.lowerBound(param, time(s)))
    (pv, lb) match {
      case (Some(p), Some(l)) => p < l
      case _ => false
    }
  }

  private def results(condition: MatchCondition): Set[Sample] =
    condition match {
      //TODO optimise and-evaluation by not evaluating unnecessary conditions?
      case and: AndMatch =>
        and.subConditions.map(results _).reduce(_ intersect _)
      case or: OrMatch =>
        or.subConditions.map(results _).reduce(_ union _)
      case at: AtomicMatch =>
        results(at)
    }

  private def doubleOption(d: java.lang.Double): Option[Double] =
    if (d == null) None else Some(d)

  private def results(condition: AtomicMatch): Set[Sample] =
    samples.filter(matches(_, condition.matchType,
        humanReadableToParam(condition.paramId),
      doubleOption(condition.param1))).toSet

  private def matches(s: Sample, mt: MatchType, param: SampleParameter,
    threshold: Option[Double]): Boolean =
    mt match {
      case MatchType.High => paramIsHigh(s, param)
      case MatchType.Low  => paramIsLow(s, param)
      case MatchType.NormalRange =>
        !paramIsHigh(s, param) && !paramIsLow(s, param)
      case _ =>
        sampleParamValue(s, param) match {
          case Some(v) =>
            mt match {
              case MatchType.AboveLimit => v >= threshold.get
              case MatchType.BelowLimit => v <= threshold.get
              case _ => throw new Exception("Unexpected match type")
            }
          case None => false
        }
    }
}
