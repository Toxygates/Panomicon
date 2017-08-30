package t.common.server.sample.search

import scala.collection.JavaConversions._
import scala.collection.Seq

import otgviewer.shared.OTGSchema
import t.common.shared.DataSchema
import t.common.shared.sample.search.AndMatch
import t.common.shared.sample.search.AtomicMatch
import t.common.shared.sample.search.MatchCondition
import t.common.shared.sample.search.MatchType
import t.common.shared.sample.search.OrMatch
import t.db.Metadata
import t.model.sample.Attribute
import t.model.sample.CoreParameter
import t.model.sample.CoreParameter.{ControlGroup => CGParam}
import t.platform.VarianceSet
import t.sparql.CachingTriplestoreMetadata
import t.sparql.SampleFilter
import t.sparql.Samples
import t.viewer.server.Annotations

  /**
   * Companion object to create sample search objects; meant to encapsulate
   * some initialization logic required in all sample search subclasses.
   * @tparam ST the type of objects that SS searches through
   * @tparam SS the type of SampleSearch object to be created
   */
trait SearchCompanion[ST, SS <: AbstractSampleSearch[ST]] {

  // Needs to be overridden to specify how to make an SS
  protected def create(metadata: Metadata, condition: MatchCondition,
             controlGroups: Map[ST, VarianceSet],
             samples: Iterable[ST],
             searchParams: Iterable[Attribute]): SS

  // Used to filter out control samples in the search space
  protected def isControlSample(schema: DataSchema): (ST => Boolean)

  // Called on samples before they are used in computations
  protected def preprocessSample(m: Metadata, sps: Iterable[Attribute]): (ST => ST)  = (x => x)

  // Finds the control groups in a collection of samples and sets up a lookup table
  protected def formControlGroups(m: Metadata, as: Annotations): (Iterable[ST] => Map[ST, VarianceSet])

  def apply(data: Samples, condition: MatchCondition, annotations: Annotations,
            samples: Iterable[ST])(implicit sf: SampleFilter): AbstractSampleSearch[ST] = {
    val schema = annotations.schema

    val attributes = annotations.baseConfig.attributes

    val usedParams = condition.neededParameters()
    val coreParams = Seq(CGParam, attributes.byId(annotations.schema.timeParameter()),
      CoreParameter.Batch, CoreParameter.SampleId)

    val neededParams = (coreParams ++ usedParams).toSeq.distinct

    val metadata = new CachingTriplestoreMetadata(data, attributes, neededParams)

    val processedSamples: Iterable[ST] = samples.map(preprocessSample(metadata, usedParams))

    val fetchedControlGroups: Map[ST, VarianceSet] = formControlGroups(metadata, annotations)(processedSamples)

    val filteredSamples: Iterable[ST] = processedSamples.filter(!isControlSample(schema)(_))

    create(metadata, condition, fetchedControlGroups, filteredSamples, usedParams)
  }
}

abstract class AbstractSampleSearch[ST](metadata: Metadata,
    condition: MatchCondition,
    controlGroups: Map[ST, VarianceSet], samples: Iterable[ST],
    searchParams: Iterable[Attribute]) {

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
                              comparator: (Double, Double) => Boolean): Option[Boolean] = {
    val testValue = paramGetter(s)
    val reference = controlGroupValue(s)
    (testValue, reference) match {
      case (Some(t), Some(r)) => Some(comparator(t, r))
      case _                  => None
    }
  }

  private def paramIsHigh(s: ST, attr: Attribute): Option[Boolean] = {
    paramComparison(s,
      sampleAttributeValue(_, attr),
      x => controlGroups.get(x).flatMap(_.upperBound(attr, zTestSampleSize(s))),
      _ > _)
  }

  private def paramIsLow(s: ST, attr: Attribute): Option[Boolean] = {
    paramComparison(s,
      sampleAttributeValue(_, attr),
      x => controlGroups.get(x).flatMap(_.lowerBound(attr, zTestSampleSize(s))),
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
        metadata.attributes.byId(condition.parameter.id),
      doubleOption(condition.param1))).toSet

  private def matches(s: ST, mt: MatchType, attr: Attribute,
                      threshold: Option[Double]): Boolean =
    mt match {
      case MatchType.High => paramIsHigh(s, attr).getOrElse(false)
      case MatchType.Low  => paramIsLow(s, attr).getOrElse(false)
      case MatchType.NormalRange =>
        !sampleAttributeValue(s, attr).isEmpty &&
          paramIsHigh(s, attr) == Some(false) &&
          paramIsLow(s, attr) == Some(false)
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
