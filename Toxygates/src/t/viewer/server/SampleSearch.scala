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
   * @param annotations source of information about the samples
   * @param samples the space of samples to search in
   */
  def ForSample(data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[Sample])(implicit sf: SampleFilter): SampleSearch[Sample] = {   
    Create[Sample](data, condition, annotations, samples, 1).apply(SampleParamValueForSample, 
        TimeForSample, PostMatchAdjustForSample, ControlGroupsForSample,
        ((m:Metadata, s:Iterable[SampleParameter]) => (u => u)))
  }
  
  /**
   * Construct a SampleSearch instance when we already have metadata, control groups, and
   * search parameters. Used in testing.
   */
  def ForSample(metadata: Metadata, condition: MatchCondition, 
      schema: DataSchema,
      controlGroups: Map[Sample, ControlGroup],
      searchParams: Iterable[SampleParameter],
      samples: Iterable[Sample]): SampleSearch[Sample] = {
    Create[Sample](metadata, condition, schema, controlGroups, searchParams, samples, 1)
        .apply(SampleParamValueForSample(metadata), TimeForSample(schema), 
            PostMatchAdjustForSample(metadata, searchParams))
  }
  /**
   * Construct a SampleSearch instance that searches Units instead of Samples
   */
  def ForUnit(data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[Unit])(implicit sf: SampleFilter): SampleSearch[Unit] = {   
    Create[Unit](data, condition, annotations, samples, 3).apply(SampleParamValueForUnit, 
        TimeForUnit, ((m:Metadata, s:Iterable[SampleParameter]) => (u => u)), ControlGroupsForUnit,
        PreprocessUnit)
  }
  
  /**
   * Creates a SampleSearch instance for a given SampleType (either Unit or Sample). 
   */
  private def Create[SampleType](data: Samples, condition: MatchCondition, annotations: Annotations,
      samples: Iterable[SampleType], zTestSampleSize: Int)(implicit sf: SampleFilter):  
      (Metadata => (SampleType, SampleParameter) => Option[Double], DataSchema => (SampleType => String),
            (Metadata, Iterable[SampleParameter]) => SampleType => SampleType,
            (Metadata, Annotations) => Iterable[SampleType] => Map[SampleType, ControlGroup],
            (Metadata, Iterable[SampleParameter]) => (SampleType => SampleType)) => SampleSearch[SampleType] = 
      (sampleParamValue: Metadata => (SampleType, SampleParameter) => Option[Double], time: DataSchema => (SampleType => String),
            postMatchAdjust: (Metadata, Iterable[SampleParameter]) => SampleType => SampleType,
            controlGroups: (Metadata, Annotations) => Iterable[SampleType] => Map[SampleType, ControlGroup],
            preprocessSamples: (Metadata, Iterable[SampleParameter]) => (SampleType => SampleType)) => {
    val schema = annotations.schema
    
    val sampleParams = annotations.baseConfig.sampleParameters
    
    val usedParams = conditionParams(sampleParams, condition) // here
    val coreParams = Seq("control_group", annotations.schema.timeParameter(), "sample_id").map(
        sampleParams.byId)
    val neededParams = (coreParams ++ usedParams).toSeq.distinct
    
    val metadata = new CachingTriplestoreMetadata(data, sampleParams, neededParams)
    
    val processedSamples = samples.map(preprocessSamples(metadata, usedParams))
    
    val fetchedControlGroups: Map[SampleType, ControlGroup] = controlGroups(metadata, annotations).apply(processedSamples)
    
    Create[SampleType](metadata, condition, schema, fetchedControlGroups, usedParams, processedSamples, zTestSampleSize).apply(sampleParamValue(metadata),
        time(schema), postMatchAdjust(metadata, usedParams))
  }

  /**
   * Creates a SampleSearch instance for a given SampleType (either Unit or Sample). 
   * Meant for when we already have metadata, control groups, etc.
   */
  private def Create[SampleType](metadata: Metadata, condition: MatchCondition, 
      schema: DataSchema, controlGroups: Map[SampleType, ControlGroup],
      searchParams: Iterable[SampleParameter], samples: Iterable[SampleType],
      zTestSampleSize: Int) =
        (sampleParamValue: (SampleType, SampleParameter) => Option[Double], time: SampleType => String,
            postMatchAdjust: SampleType => SampleType) =>
    new SampleSearch[SampleType](schema, metadata, condition, controlGroups, samples,
        searchParams, sampleParamValue, time, postMatchAdjust, zTestSampleSize)

  def conditionParams(paramSet: ParameterSet, cond: MatchCondition):
    Iterable[SampleParameter] = {
    val paramsByTitle = Map() ++
      paramSet.all.map(p => p.humanReadable -> p)
    cond.neededParameters().map(paramsByTitle)
  }
  
  /**
   * The various methods below cover everything that actually differs between a 
   * SampleSearch on a Samples and one on a Unit.
   */
  
  private def ControlGroupsForSample(metadata: Metadata, annotations:Annotations) = (samples: Iterable[Sample]) => {
    annotations.controlGroups(samples, metadata)
  }
  
  private def ControlGroupsForUnit(metadata: Metadata, annotations:Annotations) = (units: Iterable[Unit]) => {
    val sampleControlGroups = annotations.controlGroups(units.flatMap(_.getSamples()), metadata)
    Map() ++ units.map(unit => unit -> sampleControlGroups(unit.getSamples()(1)))
  }
    
  private def SampleParamValueForSample(metadata: Metadata) = (s: Sample, param: SampleParameter) => {
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
  
  private def SampleParamValueForUnit(metadata: Metadata) = (unit: Unit, param: SampleParameter) => {
    try {
      Some(unit.get(param.identifier).toDouble)
    } catch {
      case nf: NumberFormatException => None
      case np: NullPointerException => None
    }
  }
     
  private def TimeForSample(schema: DataSchema) = (s: Sample) => 
    s.get(schema.timeParameter())
  
  private def TimeForUnit(schema: DataSchema) = (unit: Unit) => 
    unit.get(schema.timeParameter())
  
  /**
   * Insert additional parameter information in the sample (the parameters
   * that were used in the match condition).
   * The mutable sample class is modified in place.
   */  
  private def PostMatchAdjustForSample(metadata: Metadata, searchParamIds: Iterable[SampleParameter]) =
    (s: Sample) => {
      val ss = asScalaSample(s)
      for (
        p <- searchParamIds;
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
   * 
   * TODO: maybe simplify this method by assuming three samples in a unit? If
   * that assumption fails we're doing the wrong Z-test anyway (since we assume
   * 3 samples there
   */
  //
  private def PreprocessUnit(metadata: Metadata, searchParams: Iterable[SampleParameter]) =
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
class SampleSearch[SampleType](schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[SampleType, ControlGroup],
    samples: Iterable[SampleType],
    searchParams: Iterable[SampleParameter],
    sampleParamValue: (SampleType, SampleParameter) => Option[Double],
    time: SampleType => String,
    postMatchAdjust: SampleType => SampleType,
    zTestSampleSize: Int) {
  
  val humanReadableToParam = Map() ++ metadata.parameterSet.all.map(p =>
    p.humanReadable -> p)

  /**
   * Results of the search.
   */
  lazy val results: Iterable[SampleType] =
    results(condition).toSeq.map(postMatchAdjust)
  
  private def paramComparison(s: SampleType, param: SampleParameter,
      paramGetter: SampleType => Option[Double],
      controlGroupValue: SampleType => Option[Double],
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
  
  private def paramIsHigh(s: SampleType, param: SampleParameter): Boolean = {
    paramComparison(s, param,
      (x => sampleParamValue(x, param)),
      (x => controlGroups.get(x).flatMap(_.upperBound(param, time(x), zTestSampleSize))),
      ((x: Double, y: Double) => x > y))
  }

  private def paramIsLow(s: SampleType, param: SampleParameter): Boolean = {
    paramComparison(s, param,
      (x => sampleParamValue(x, param)),
      (x => controlGroups.get(x).flatMap(_.lowerBound(param, time(x), zTestSampleSize))),
      ((x: Double, y: Double) => x < y))
  }

  private def results(condition: MatchCondition): Set[SampleType] =
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

  private def results(condition: AtomicMatch): Set[SampleType] =
    samples.filter(matches(_, condition.matchType,
        humanReadableToParam(condition.paramId),
      doubleOption(condition.param1))).toSet
      
  private def matches(s: SampleType, mt: MatchType, param: SampleParameter,
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
