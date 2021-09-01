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

package t.server.common.sample.search

import t.model.sample.OTGAttribute._
import t.shared.common.DataSchema
import t.shared.common.sample.Sample
import t.shared.common.sample.search.MatchCondition
import t.db.SimpleVarianceSet
import t.model.SampleClass
import t.model.sample.{AttributeSet, CoreParameter, VarianceSet}
import t.sparql.SampleFilter
import t.sparql.SampleStore
import t.server.viewer.UnitsHelper
import t.shared.common.sample.Unit

object IndividualSearch extends SearchCompanion[Sample, IndividualSearch] {

  def apply(samples: Iterable[Sample], condition: MatchCondition,
      unitsHelper: UnitsHelper, attributes: AttributeSet) = {

    val unitsAndVarianceSets = Map() ++
      unitsHelper.formControlUnitsAndVarianceSets(samples).flatMap {
        case (treatedUnit, (controlUnit, varianceSet)) =>
          treatedUnit.getSamples.map(_ -> ((treatedUnit, controlUnit), varianceSet))
    }

    val treatedSamples = unitsAndVarianceSets.map(_._1)
    val unitsAndVarianceSetsById = Map() ++ unitsAndVarianceSets.map {
      case(sample, stuff) =>
        sample.get(CoreParameter.SampleId) -> stuff
    }

    val units = unitsAndVarianceSetsById.mapValues(_._1)
    val varianceSets = unitsAndVarianceSetsById.mapValues(_._2)

    new IndividualSearch(condition, varianceSets, units, treatedSamples)
  }
}

class IndividualSearch(condition: MatchCondition, varianceSets: Map[String, VarianceSet],
                       units: Map[String, (Unit, Unit)], samples: Iterable[Sample])
    extends AbstractSampleSearch[Sample](condition, varianceSets, samples)  {

  lazy val pairedResults = results.map(sample => (sample,
      units(sample.get(CoreParameter.SampleId))))

  protected def zTestSampleSize(s: Sample): Int = 1

  protected def sortObject(sample: Sample): (String, Int, Int) = {
    (sample.get(Compound), doseLevelMap.getOrElse((sample.get(DoseLevel)), Int.MaxValue),
        exposureTimeMap.getOrElse((sample.get(ExposureTime)), Int.MaxValue))
  }
}
