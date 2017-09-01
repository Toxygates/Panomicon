package t.common.server.sample.search

import otg.model.sample.OTGAttribute._
import t.common.shared.DataSchema
import t.common.shared.sample.Sample
import t.common.shared.sample.search.MatchCondition
import t.db.SimpleVarianceSet
import t.db.VarianceSet
import t.model.SampleClass
import t.model.sample.AttributeSet
import t.model.sample.CoreParameter
import t.sparql.SampleFilter
import t.sparql.Samples
import t.viewer.server.UnitsHelper

object IndividualSearch extends SearchCompanion[Sample, IndividualSearch] {

  def apply(condition: MatchCondition, sampleClass: SampleClass, sampleStore: Samples,
      schema: DataSchema, attributes: AttributeSet)
      (implicit sampleFilter: SampleFilter): IndividualSearch = {

    val unitHelper = new UnitsHelper(schema)
    val samples = rawSamples(condition, sampleClass, sampleFilter,
        sampleStore, schema, attributes)
    val groupedSamples = unitHelper.formTreatedAndControlUnits(samples)

    val treatedSamples = groupedSamples.flatMap(_._1).flatten
    val varianceSets = Map() ++ groupedSamples.flatMap { case (treatedSamples, controlSamples) =>
      val varianceSet = new SimpleVarianceSet(controlSamples)
      treatedSamples.flatMap(_.map(_.get(CoreParameter.SampleId) -> varianceSet))
    }

    new IndividualSearch(condition, varianceSets, treatedSamples)
  }
}

class IndividualSearch(condition: MatchCondition,
    varianceSets: Map[String, VarianceSet], samples: Iterable[Sample])
    extends AbstractSampleSearch[Sample](condition, varianceSets, samples)  {

  protected def zTestSampleSize(s: Sample): Int = 1

  protected def sortObject(sample: Sample): (String, Int, Int) = {
    (sample.get(Compound), doseLevelMap.getOrElse((sample.get(DoseLevel)), Int.MaxValue),
        exposureTimeMap.getOrElse((sample.get(ExposureTime)), Int.MaxValue))
  }
}
