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

object SampleSearch {

  /**
   * Construct a SampleSearch instance.
   * @param condition the criteria to search for
   * @param samples the space of samples to search in
   * @param annotations source of information about the samples
   */
  def apply(condition: MatchCondition, annotations: Annotations,
      samples: Iterable[Sample])(implicit sf: SampleFilter): SampleSearch = {

    //Attempt to speed up the search by only querying the parameters needed from the
    //metadata store.
    //TODO other parameters needed later?

    val sampleParams = annotations.baseConfig.sampleParameters

    val paramsByTitle = Map() ++
      sampleParams.all.map(p => p.humanReadable -> p)

    val conditionParams = condition.neededParameters().map(paramsByTitle)
    val coreParams = Seq("control_group", "exposure_time", "sample_id").map(
        sampleParams.byId)
    val neededParams = (coreParams ++ conditionParams).toSeq.distinct

    val metadata = new CachingTriplestoreMetadata(annotations.sampleStore,
        sampleParams, neededParams)
    val controlGroups = annotations.controlGroups(samples, metadata)
    new SampleSearch(annotations.schema, metadata, condition, controlGroups, samples,
        conditionParams)
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
  val humanReadableToParamId = Map() ++ metadata.parameters.all.map(p =>
    p.humanReadable -> p.identifier)

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
      v <- metadata.getParameter(ss, p)) {
      s.sampleClass().put(p, v)
    }
    s
  }

  //TODO
  private def sampleParamValue(s: Sample, param: String): Option[Double] =
    try {
      metadata.getParameter(asScalaSample(s), param) match {
        case Some("NA") => None
        case Some(s) => Some(s.toDouble)
        case None => None
      }
    } catch {
      case nf: NumberFormatException => None
    }

  def time(s: Sample): String = s.get(schema.timeParameter())

  private def paramIsHigh(s: Sample, param: String): Boolean = {
    val pv = sampleParamValue(s, param)
    val ub = controlGroups.get(s).flatMap(_.upperBound(param, time(s)))
    (pv, ub) match {
      case (Some(p), Some(u)) => p > u
      case _ => false
    }
  }

  private def paramIsLow(s: Sample, param: String): Boolean = {
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
        humanReadableToParamId(condition.paramId),
      doubleOption(condition.param1))).toSet

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
              case _ => throw new Exception("Unexpected match type")
            }
          case None => false
        }
    }
}
