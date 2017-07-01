package t.viewer.server

import Conversions._
import t.db.SampleParameter
import t.platform.ControlGroup
import org.stringtemplate.v4.ST
import t.common.shared.DataSchema
import t.common.shared.sample.Sample
import t.common.shared.sample.search.MatchCondition
import t.db.Metadata

object IndividualSearch extends SearchCompanion[Sample, IndividualSearch] {

  def create(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, ControlGroup],
    samples: Iterable[Sample],
    searchParams: Iterable[SampleParameter]) =
      new IndividualSearch(schema, metadata, condition, controlGroups, samples, searchParams)

  def preprocessSample(metadata: Metadata, searchParams: Iterable[SampleParameter]) =
    (sample: Sample) => sample

  def formControlGroups(metadata: Metadata, annotations:Annotations) = (samples: Iterable[Sample]) =>
    annotations.controlGroups(samples, metadata)

}

class IndividualSearch(schema: DataSchema, metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, ControlGroup], samples: Iterable[Sample], searchParams: Iterable[SampleParameter])
    extends AbstractSampleSearch[Sample](schema, metadata, condition,
        controlGroups, samples, searchParams)  {

  def sampleParamValue(sample: Sample, param: SampleParameter): Option[Double] = {
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

  def time(sample: Sample): String =
    sample.get(schema.timeParameter())

  /**
   * Insert additional parameter information in the sample (the parameters
   * that were used in the match condition).
   * The mutable sample class is modified in place.
   */
  def postMatchAdjust(sample: Sample): Sample = {
      val ss = asScalaSample(sample)
      for (
        p <- searchParams;
        v <- metadata.parameter(ss, p.identifier)
      ) {
        sample.sampleClass().put(p.identifier, v)
      }
      sample
    }

  val zTestSampleSize = 1
}