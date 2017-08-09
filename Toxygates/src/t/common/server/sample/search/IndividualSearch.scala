package t.common.server.sample.search

import t.viewer.server
import t.viewer.server.Conversions._
import t.viewer.server.Annotations
import t.db.SampleParameter
import t.platform.ControlGroup
import t.common.shared.DataSchema
import t.common.shared.sample.Sample
import t.common.shared.sample.search.MatchCondition
import t.db.Metadata

object IndividualSearch extends SearchCompanion[Sample, IndividualSearch] {

  protected def create(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, ControlGroup],
    samples: Iterable[Sample],
    searchParams: Iterable[SampleParameter]) =
      new IndividualSearch(schema, metadata, condition, controlGroups, samples, searchParams)

  protected def formControlGroups(metadata: Metadata, annotations:Annotations) =
    annotations.controlGroups(_, metadata)
}

class IndividualSearch(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, ControlGroup], samples: Iterable[Sample], searchParams: Iterable[SampleParameter])
    extends AbstractSampleSearch[Sample](schema, metadata, condition,
        controlGroups, samples, searchParams)  {

  protected def sampleParamValue(sample: Sample, param: SampleParameter): Option[Double] = {
    try {
      metadata.parameter(asScalaSample(sample), param.identifier) match {
        case Some("NA") => None
        case Some(s)    => Some(s.toDouble)
        case None       => None
      }
    } catch {
      case nf: NumberFormatException => None
    }
  }

  protected def time(sample: Sample): String =
    sample.get(schema.timeParameter())

  /**
   * Insert additional parameter information in the sample (the parameters
   * that were used in the match condition).
   * The mutable sample class is modified in place.
   */
  protected def postMatchAdjust(sample: Sample): Sample = {
      val ss = asScalaSample(sample)
      for (
        p <- searchParams;
        v <- metadata.parameter(ss, p.identifier)
      ) {
        sample.sampleClass().put(p.identifier, v)
      }
      sample
    }

  protected def zTestSampleSize(s: Sample): Int = 1

  protected def sortObject(sample: Sample): (String, Int, Int) = {
    (sample.get("compound_name"), doseLevelMap(sample.get("dose_level")),
        exposureTimeMap(sample.get("exposure_time")))
  }
}
