/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.common.server.sample.search

import scala.collection.JavaConverters._

import otg.model.sample.OTGAttribute._
import t.common.shared.sample.Sample
import t.common.shared.sample.Unit
import t.common.shared.sample.search.MatchCondition
import t.db.VarianceSet
import t.model.sample.AttributeSet
import t.model.sample.CoreParameter
import t.viewer.server.UnitsHelper

object UnitSearch extends SearchCompanion[Unit, UnitSearch] {

  def apply(samples: Iterable[Sample], condition: MatchCondition,
      unitsHelper: UnitsHelper, attributes: AttributeSet) = {

    val controlUnitsAndVarianceSets = unitsHelper.formControlUnitsAndVarianceSets(samples)

    val units = controlUnitsAndVarianceSets.map(_._1)
    units.map(_.concatenateAttribute(CoreParameter.SampleId))
    units.map(u => condition.neededParameters.asScala.map(u.averageAttribute(_)))

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
