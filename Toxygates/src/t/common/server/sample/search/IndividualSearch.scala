package t.common.server.sample.search

import t.viewer.server
import t.viewer.server.Conversions._
import t.viewer.server.Annotations
import t.db.VarianceSet
import t.common.shared.DataSchema
import t.common.shared.sample.Sample
import t.common.shared.sample.search.MatchCondition
import t.db.Metadata
import t.model.sample.Attribute
import otg.model.sample.OTGAttribute._

object IndividualSearch extends SearchCompanion[Sample, IndividualSearch] {

  protected def create(metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, VarianceSet],
    samples: Iterable[Sample],
    searchParams: Iterable[Attribute]) =
      new IndividualSearch(metadata, condition, controlGroups, samples, searchParams)

  protected def formControlGroups(metadata: Metadata, annotations: Annotations) =
    annotations.controlGroups(_, metadata)

  protected def isControlSample(schema: DataSchema) =
    schema.isControl(_)
}

class IndividualSearch(metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Sample, VarianceSet], samples: Iterable[Sample], searchParams: Iterable[Attribute])
    extends AbstractSampleSearch[Sample](metadata, condition,
        controlGroups, samples, searchParams)  {

  protected def sampleAttributeValue(sample: Sample, attr: Attribute): Option[Double] = {
    try {
      metadata.parameter(asScalaSample(sample), attr) match {
        case Some("NA") => None
        case Some(s)    => Some(s.toDouble)
        case None       => None
      }
    } catch {
      case nf: NumberFormatException => None
    }
  }

  /**
   * Insert additional parameter information in the sample (the parameters
   * that were used in the match condition).
   * The mutable sample class is modified in place.
   */
  protected def postMatchAdjust(sample: Sample): Sample = {
      val ss = asScalaSample(sample)
      for (
        p <- searchParams;
        v <- metadata.parameter(ss, p)
      ) {
        sample.sampleClass().put(p, v)
      }
      sample
    }

  protected def zTestSampleSize(s: Sample): Int = 1

  protected def sortObject(sample: Sample): (String, Int, Int) = {
    (sample.get(Compound), doseLevelMap.getOrElse((sample.get(DoseLevel)), Int.MaxValue),
        exposureTimeMap.getOrElse((sample.get(ExposureTime)), Int.MaxValue))
  }
}
