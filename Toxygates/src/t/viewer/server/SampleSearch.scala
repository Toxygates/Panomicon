package t.viewer.server

import t.common.shared.sample.search._

import scala.collection.JavaConversions._
import t.platform.ControlGroup
import t.common.shared.DataSchema
import t.db.Metadata
import Conversions._
import t.common.shared.sample.Sample
import t.common.shared.sample.Unit
import t.sparql.TriplestoreMetadata
import t.sparql.SampleFilter
import t.sparql.CachingTriplestoreMetadata
import t.db.SampleParameter
import t.sparql.Samples
import t.sample.SampleSet
import t.db.ParameterSet
import t.common.shared.sample.BioParamValue
import t.model.sample.CoreParameter._

@deprecated("refactored", "1 July 2017")
object SampleSearch {

  /**
   * Construct a SampleSearch instance.
   * @param condition the criteria to search for
   * @param annotations source of information about the samples
   * @param samples the space of samples to search in
   */
  def forSample(data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[Sample])(implicit sf: SampleFilter): SampleSearch[Sample] = {
    create[Sample](data, condition, annotations, samples, 1).apply(sampleParamValueForSample,
        timeForSample, postMatchAdjustForSample, controlGroupsForSample,
        ((m:Metadata, s:Iterable[SampleParameter]) => (u => u)))
  }

  /**
   * Construct a SampleSearch instance when we already have metadata, control groups, and
   * search parameters. Used in testing.
   */
  def forSample(metadata: Metadata, condition: MatchCondition,
      schema: DataSchema,
      controlGroups: Map[Sample, ControlGroup],
      searchParams: Iterable[SampleParameter],
      samples: Iterable[Sample]): SampleSearch[Sample] = {
    createWithMetadata[Sample](metadata, condition, schema, controlGroups, searchParams, samples, 1)
        .apply(sampleParamValueForSample(metadata), timeForSample(schema),
            postMatchAdjustForSample(metadata, searchParams))
  }
  /**
   * Construct a SampleSearch instance that searches Units instead of Samples
   */
  def forUnit(data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[Unit])(implicit sf: SampleFilter): SampleSearch[Unit] = {
    create[Unit](data, condition, annotations, samples, 3).apply(sampleParamValueForUnit,
        timeForUnit, ((m:Metadata, s:Iterable[SampleParameter]) => (u => u)), controlGroupsForUnit,
        preprocessUnit)
  }

  /**
   * Creates a SampleSearch instance for a given SampleType (either Unit or Sample).
   */
  private def create[SampleType](data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[SampleType], zTestSampleSize: Int)(implicit sf: SampleFilter) =
      (sampleParamValue: Metadata => (SampleType, SampleParameter) => Option[Double], time: DataSchema => (SampleType => String),
            postMatchAdjust: (Metadata, Iterable[SampleParameter]) => SampleType => SampleType,
            controlGroups: (Metadata, Annotations) => Iterable[SampleType] => Map[SampleType, ControlGroup],
            preprocessSamples: (Metadata, Iterable[SampleParameter]) => (SampleType => SampleType)) => {
    val schema = annotations.schema

    val sampleParams = annotations.baseConfig.sampleParameters

    val usedParams = conditionParams(sampleParams, condition) // here
    val coreParams = Seq(ControlGroup.id, annotations.schema.timeParameter(), "sample_id").map(
        sampleParams.byId)
    val neededParams = (coreParams ++ usedParams).toSeq.distinct

    val metadata = new CachingTriplestoreMetadata(data, sampleParams, neededParams)

    val processedSamples = samples.map(preprocessSamples(metadata, usedParams))

    val fetchedControlGroups: Map[SampleType, ControlGroup] = controlGroups(metadata, annotations).apply(processedSamples)

    createWithMetadata[SampleType](metadata, condition, schema, fetchedControlGroups, usedParams, processedSamples, zTestSampleSize).apply(sampleParamValue(metadata),
        time(schema), postMatchAdjust(metadata, usedParams))
  }

  /**
   * Creates a SampleSearch instance for a given SampleType (either Unit or Sample).
   * Meant for when we already have metadata, control groups, etc.
   */
  private def createWithMetadata[SampleType](metadata: Metadata, condition: MatchCondition,
      schema: DataSchema, controlGroups: Map[SampleType, ControlGroup],
      searchParams: Iterable[SampleParameter], samples: Iterable[SampleType],
      zTestSampleSize: Int) =
        (sampleParamValue: (SampleType, SampleParameter) => Option[Double], time: SampleType => String,
            postMatchAdjust: SampleType => SampleType) =>
    new SampleSearch[SampleType](schema, metadata, condition, controlGroups, samples,
        searchParams, sampleParamValue, time, postMatchAdjust, zTestSampleSize)

  def conditionParams(paramSet: ParameterSet, cond: MatchCondition):
    Iterable[SampleParameter] = {
    cond.neededParameters().map(p => paramSet.byId(p.id))
  }

  /**
   * The various methods below cover everything that actually differs between a
   * SampleSearch on a Samples and one on a Unit.
   */

  private def controlGroupsForSample(metadata: Metadata, annotations:Annotations) = (samples: Iterable[Sample]) => {
    annotations.controlGroups(samples, metadata)
  }

  private def controlGroupsForUnit(metadata: Metadata, annotations:Annotations) = (units: Iterable[Unit]) => {
    val sampleControlGroups = annotations.controlGroups(units.flatMap(_.getSamples()), metadata)
    Map() ++ units.map(unit => unit -> sampleControlGroups(unit.getSamples()(1)))
  }

  private def sampleParamValueForSample(metadata: Metadata) = (s: Sample, param: SampleParameter) => {
    try {
      metadata.parameter(asScalaSample(s), param.identifier) match {
        case Some("NA") => None
        case Some(s)    => Some(s.toDouble)
        case None       => None
      }
    } catch {
      case nf: NumberFormatException => None
    }
  }

  private def sampleParamValueForUnit(metadata: Metadata) = (unit: Unit, param: SampleParameter) => {
    try {
      Some(unit.get(param.identifier).toDouble)
    } catch {
      case nf: NumberFormatException => None
      case np: NullPointerException => None
    }
  }

  private def timeForSample(schema: DataSchema) = (s: Sample) =>
    s.get(schema.timeParameter())

  private def timeForUnit(schema: DataSchema) = (unit: Unit) =>
    unit.get(schema.timeParameter())

  /**
   * Insert additional parameter information in the sample (the parameters
   * that were used in the match condition).
   * The mutable sample class is modified in place.
   */
  private def postMatchAdjustForSample(metadata: Metadata, searchParams: Iterable[SampleParameter]) =
    (s: Sample) => {
      val ss = asScalaSample(s)
      for (
        p <- searchParams;
        v <- metadata.parameter(ss, p.identifier)
      ) {
        s.sampleClass().put(p.identifier, v)
      }
      s
    }

  /**
   * Preprocess a Unit to prepare it for searching. For each search parameter,
   * computes the average value for samples in the unit, and stores it as the
   * parameter value for the unit.
   */
  //
  private def preprocessUnit(metadata: Metadata, searchParams: Iterable[SampleParameter]) =
    (unit: Unit) => {
      val samples = unit.getSamples
      for (param <- searchParams) {
        val paramId = param.identifier

        unit.put(paramId, try {
          var sum: Option[Double] = None
          var count: Int = 0;

          for (sample <- samples) {
            val scalaSample = asScalaSample(sample)

            sum = metadata.parameter(scalaSample, paramId) match {
              case Some("NA") => sum
              case Some(str)  => {
                count = count + 1
                sum match {
                  case Some(x) => Some(x + str.toDouble)
                  case None    => Some(str.toDouble)
                }
              }
              case None       => sum
            }
          }

          sum match {
            case Some(x) => {
              (x / count).toString()
            }
            case None    => null
          }
        } catch {
          case nf: NumberFormatException => null
        })
      }
      unit
    }
}

/**
 * Evaluates sample searches for a match condition.
 */
@deprecated("refactored", "1 July 2017")
class SampleSearch[ST](schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[ST, ControlGroup],
    samples: Iterable[ST],
    searchParams: Iterable[SampleParameter],
    sampleParamValue: (ST, SampleParameter) => Option[Double],
    time: ST => String,
    postMatchAdjust: ST => ST,
    zTestSampleSize: Int) {

  val humanReadableToParam = Map() ++ metadata.parameterSet.all.map(p =>
    p.humanReadable -> p)

  /**
   * Results of the search.
   */
  lazy val results: Iterable[ST] =
    results(condition).toSeq.map(postMatchAdjust)

  private def paramComparison(s: ST, param: SampleParameter,
      paramGetter: ST => Option[Double],
      controlGroupValue: ST => Option[Double],
      comparator: (Double, Double) => Boolean): Boolean = {
    val pv = paramGetter(s)
    val ub = controlGroupValue(s)
    (pv, ub) match {
      case (Some(p), Some(u)) => comparator(p, u)
      case (Some(p), None) => false
      case (None, Some(u)) => false
      case _ => false
    }
  }

  private def paramIsHigh(s: ST, param: SampleParameter): Boolean = {
    paramComparison(s, param,
      (x => sampleParamValue(x, param)),
      (x => controlGroups.get(x).flatMap(_.upperBound(param, time(x), zTestSampleSize))),
      ((x: Double, y: Double) => x > y))
  }

  private def paramIsLow(s: ST, param: SampleParameter): Boolean = {
    paramComparison(s, param,
      (x => sampleParamValue(x, param)),
      (x => controlGroups.get(x).flatMap(_.lowerBound(param, time(x), zTestSampleSize))),
      ((x: Double, y: Double) => x < y))
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

  private def paramLookup(p: BioParamValue) =
    metadata.parameterSet.byId(p.id)

  private def results(condition: AtomicMatch): Set[ST] =
    samples.filter(matches(_, condition.matchType,
      paramLookup(condition.parameter),
      doubleOption(condition.param1))).toSet

  private def matches(s: ST, mt: MatchType, param: SampleParameter,
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
