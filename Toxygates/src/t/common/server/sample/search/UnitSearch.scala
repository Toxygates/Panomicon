package t.common.server.sample.search

import scala.collection.JavaConversions._

import otg.model.sample.OTGAttribute._
import t.common.shared.DataSchema
import t.common.shared.sample.Unit
import t.common.shared.sample.search.MatchCondition
import t.db.SimpleVarianceSet
import t.db.VarianceSet
import t.model.SampleClass
import t.model.sample.AttributeSet
import t.model.sample.CoreParameter
import t.sparql.SampleFilter
import t.sparql.Samples
import t.viewer.server.UnitsHelper
import t.common.shared.sample.Sample

object UnitSearch extends SearchCompanion[Unit, UnitSearch] {

  def apply(samples: Iterable[Sample], condition: MatchCondition,
      unitsHelper: UnitsHelper, attributes: AttributeSet) = {

    val controlUnitsAndVarianceSets = unitsHelper.formControlUnitsAndVarianceSets(samples)

    val units = controlUnitsAndVarianceSets.map(_._1)
    units.map(_.concatenateAttribute(CoreParameter.SampleId))
    units.map(u => condition.neededParameters.map(u.averageAttribute(_)))

    val controlUnitsAndVarianceSetsbyID = Map() ++
        controlUnitsAndVarianceSets.map { case (unit, stuff) =>
          unit.get(CoreParameter.SampleId) -> stuff
        }
    val controlUnits = controlUnitsAndVarianceSetsbyID.mapValues(_._1)
    val varianceSets = controlUnitsAndVarianceSetsbyID.mapValues(_._2)

    new UnitSearch(condition, varianceSets, controlUnits, units, attributes)
  }
}

class UnitSearch(condition: MatchCondition,
    varianceSets: Map[String, VarianceSet], controlUnits: Map[String, Unit],
    samples: Iterable[Unit], attributes: AttributeSet)
    extends AbstractSampleSearch[Unit](condition, varianceSets, samples)  {

  lazy val pairedResults = results.map(unit => (unit,
      controlUnits(unit.get(CoreParameter.SampleId))))

  override protected def postProcessSample(sample: Unit): Unit = {
    sample.computeAllAttributes(attributes, false)
    sample
  }

  protected def zTestSampleSize(unit: Unit): Int =
    unit.getSamples().length

  protected def sortObject(unit: Unit): (String, Int, Int) = {
    (unit.get(Compound), doseLevelMap.getOrElse((unit.get(DoseLevel)), Int.MaxValue),
        exposureTimeMap.getOrElse((unit.get(ExposureTime)), Int.MaxValue))
  }
}
