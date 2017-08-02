package t.common.server.sample.search

import scala.collection.JavaConversions._
import t.platform.ControlGroup
import t.common.shared.DataSchema
import t.common.shared.sample.search.AndMatch
import t.common.shared.sample.search.AtomicMatch
import t.common.shared.sample.search.OrMatch
import t.common.shared.sample.search.MatchCondition
import t.common.shared.sample.search.MatchType
import t.db.Metadata
import t.sparql.Samples
import t.sparql.CachingTriplestoreMetadata
import t.sparql.SampleFilter
import t.platform.ControlGroup
import t.db.ParameterSet
import scala.collection.Seq
import t.viewer.server.Annotations
import otgviewer.shared.OTGSchema
import t.model.sample.CoreParameter.{ControlGroup => CGParam}
import scala.annotation.meta.param
import t.model.sample.Attribute
import scala.annotation.meta.param

  /**
   * Companion object to create sample search objects; meant to encapsulate
   * some initialization logic required in all sample search subclasses.
   * @tparam ST the type of objects that SS searches through
   * @tparam SS the type of SampleSearch object to be created
   */
trait SearchCompanion[ST, SS <: AbstractSampleSearch[ST]] {

  // Needs to be overridden to specify how to make an SS
  protected def create(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
             controlGroups: Map[ST, ControlGroup],
             samples: Iterable[ST],
             searchParams: Iterable[Attribute]): SS

  // Called on samples before they are used in computations
  protected def preprocessSample(m: Metadata, sps: Iterable[Attribute]): (ST => ST)

  // Finds the control groups in a collection of samples and sets up a lookup table
  protected def formControlGroups(m: Metadata, as: Annotations): (Iterable[ST] => Map[ST, ControlGroup])

  // Extracts the sample parameters used in a MatchCondition
  private def conditionParams(paramSet: ParameterSet, cond: MatchCondition): Iterable[Attribute] = {
    cond.neededParameters().map(p => paramSet.byId(p.id))
  }

  def apply(data: Samples, condition: MatchCondition, annotations: Annotations,
            samples: Iterable[ST])(implicit sf: SampleFilter): AbstractSampleSearch[ST] = {
    val schema = annotations.schema

    val sampleParams = annotations.baseConfig.sampleParameters

    val usedParams = conditionParams(sampleParams, condition) // here
    val coreParams = Seq(CGParam.id, annotations.schema.timeParameter(), "sample_id").map(
      sampleParams.byId)
    val neededParams = (coreParams ++ usedParams).toSeq.distinct

    val metadata = new CachingTriplestoreMetadata(data, sampleParams, neededParams)

    val processedSamples: Iterable[ST] = samples.map(preprocessSample(metadata, usedParams))

    val fetchedControlGroups: Map[ST, ControlGroup] = formControlGroups(metadata, annotations)(processedSamples)

    create(schema, metadata, condition, fetchedControlGroups, samples, usedParams)
  }
}

abstract class AbstractSampleSearch[ST](schema: DataSchema, metadata: Metadata,
    condition: MatchCondition,
    controlGroups: Map[ST, ControlGroup], samples: Iterable[ST],
    searchParams: Iterable[Attribute]) {

  protected def time(s: ST): String
  protected def sampleAttributeValue(s: ST, sp: Attribute): Option[Double]
  protected def postMatchAdjust(s: ST): ST
  protected def zTestSampleSize(s: ST): Int
  protected def sortObject(s: ST): (String, Int, Int)

  /**
   * Results of the search.
   */
  lazy val results: Iterable[ST] =
    results(condition).toSeq.map(postMatchAdjust).sortBy(sortObject(_))

  private def paramComparison(s: ST,
                              paramGetter: ST => Option[Double],
                              controlGroupValue: ST => Option[Double],
                              comparator: (Double, Double) => Boolean): Boolean = {
    val pv = paramGetter(s)
    val ub = controlGroupValue(s)
    (pv, ub) match {
      case (Some(p), Some(u)) => comparator(p, u)
      case _                  => false
    }
  }

  private def paramIsHigh(s: ST, attr: Attribute): Boolean = {
    paramComparison(s,
      sampleAttributeValue(_, attr),
      x => controlGroups.get(x).flatMap(_.upperBound(attr, time(x), zTestSampleSize(s))),
      _ > _)
  }

  private def paramIsLow(s: ST, attr: Attribute): Boolean = {
    paramComparison(s,
      sampleAttributeValue(_, attr),
      x => controlGroups.get(x).flatMap(_.lowerBound(attr, time(x), zTestSampleSize(s))),
      _ < _)
  }

  private def results(condition: MatchCondition): Set[ST] =
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

  private def results(condition: AtomicMatch): Set[ST] =
    samples.filter(matches(_, condition.matchType,
        metadata.parameterSet.byId(condition.parameter.id),
      doubleOption(condition.param1))).toSet

  private def matches(s: ST, mt: MatchType, attr: Attribute,
                      threshold: Option[Double]): Boolean =
    mt match {
      case MatchType.High => paramIsHigh(s, attr)
      case MatchType.Low  => paramIsLow(s, attr)
      case MatchType.NormalRange =>
        !paramIsHigh(s, attr) && !paramIsLow(s, attr)
      case _ =>
        sampleAttributeValue(s, attr) match {
          case Some(v) =>
            mt match {
              case MatchType.AboveLimit => v >= threshold.get
              case MatchType.BelowLimit => v <= threshold.get
              case _                    => throw new Exception("Unexpected match type")
            }
          case None => false
        }
    }

  protected def doseLevelMap: Map[String, Int] =
    Map() ++ OTGSchema.allDoses.zipWithIndex

  protected def exposureTimeMap: Map[String, Int] =
    Map() ++ OTGSchema.allTimes.zipWithIndex
}
