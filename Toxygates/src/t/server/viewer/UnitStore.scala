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

package t.server.viewer

import t.shared.common.DataSchema
import t.shared.common.Pair
import t.shared.common.sample.Sample
import t.shared.common.sample.SampleClassUtils
import t.shared.common.sample.Unit
import t.db.SimpleVarianceSet
import t.model.SampleClass
import t.model.sample.CoreParameter._
import t.model.sample.VarianceSet
import t.sparql.SampleClassFilter
import t.sparql.SampleFilter
import t.sparql.SampleStore
import t.server.viewer.Conversions._
import t.shared.viewer.TimeoutException

object UnitStore {
  def asUnit(ss: Iterable[Sample], schema: DataSchema) = new Unit(
    SampleClassUtils.asUnit(ss.head.sampleClass, schema), ss.toArray)

  def isControl(s: t.db.Sample) =
    s.get(ControlTreatment) == s.get(Treatment)
}

class UnitStore(schema: DataSchema, sampleStore: SampleStore) extends
  UnitsHelper(schema) {
  import UnitStore._

  /**
   * For a user-specified sample filter, searches for samples and
   * generates units containing treated samples and their associated control samples.
   */
  @throws[TimeoutException]
  def units(sc: SampleClass, param: String, paramValues: Array[String],
            sf: SampleFilter) : Array[Pair[Unit, Unit]] = {

    //This will filter by the chosen parameter - usually compound name
    //The result need not contain the corresponding control samples, so these must be fetched
    //using a separate query.
    val rs = sampleStore.samples(SampleClassFilter(sc), param,
      paramValues.toSeq, sf)
    units(sc, rs, sf).map(x => new Pair(x._1, x._2.getOrElse(null))).toArray
  }

  def units(sc: SampleClass, samples: Iterable[t.db.Sample],
            sf: SampleFilter): Iterable[(Unit, Option[Unit])] = {

    val groupedSamples = samples.groupBy(x => (x.sampleClass(Batch), x.sampleClass(ControlTreatment)))

    val controlTreatments = groupedSamples.keys.toSeq.map(_._2).distinct
    //Look for samples where Treatment is in the set of expected ControlTreatment values
    val potentialControls = sampleStore.samples(SampleClassFilter(sc),
      Treatment.id, controlTreatments, sf).map(asJavaSample)

    var r = List[(Unit, Option[Unit])]()
    for {
      ((batch, controlTreatment), samples) <- groupedSamples;
      treated = samples.filter(!isControl(_)).map(asJavaSample)
      (treatment, treatedSamples) <- treated.groupBy(_.get(Treatment))
    } {

      val filteredControls = potentialControls.filter(s =>
        s.get(Treatment) == controlTreatment && s.get(Batch) == batch)

      val controlUnit = if (filteredControls.isEmpty) {
        None
      } else {
        Some(asUnit(filteredControls, schema))
      }
      val treatedUnit = asUnit(treatedSamples, schema)
      r ::= (treatedUnit, controlUnit)

      for {cu <- controlUnit} {
        //pseudo-unit
        r ::= (cu, None)
      }
    }
    r
  }
}

class UnitsHelper(schema: DataSchema) {
  import UnitStore._

  def samplesByControlGroup(raw: Iterable[Sample]): Map[String, Iterable[Sample]] =
    raw.groupBy(_.get(ControlTreatment))

  def controlGroupKey(s: Sample): String = s.get(ControlTreatment)

  def unitGroupKey(s: Sample) = s.get(schema.mediumParameter())

  /**
   * Groups the samples provided into treated and control units, returning
   * a list of tuples whose first element is a treated unit and whose second
   * element is a tuple containing the corresponding control unit and variance
   * set.
   * @param samples samples to partition
   */
  def formControlUnitsAndVarianceSets(samples: Iterable[Sample]): Seq[(Unit, (Unit, VarianceSet))] =
    for {
      (treatedSamples, controlSamples) <- formTreatedAndControlUnits(samples).toList
      if treatedSamples.nonEmpty && controlSamples.nonEmpty
      controlGroup = asUnit(controlSamples, schema)
      varianceSet = new SimpleVarianceSet(controlSamples)
      unit <- treatedSamples.map(asUnit(_, schema))
    } yield (unit, (controlGroup, varianceSet))

  /**
   * Groups the samples provided into treated and control groups, returning
   * a list of tuples whose first element is the samples in a treated unit and
   * whose second element is the samples from the corresponding control unit.
   * @param samples samples to partition
   */
  def formTreatedAndControlUnits(samples: Iterable[Sample]):
      Seq[(Iterable[Iterable[Sample]], Iterable[Sample])] = {
    def isControl(s: Sample) = s.get(ControlTreatment) == s.get(Treatment)
    val controlGroups = samples.groupBy(_.get(ControlTreatment))

    for {
      (unitKey, samples) <- controlGroups.toList
      (treated, controls) = samples.partition(!isControl(_))
      grouped = treated.groupBy(_.get(Treatment)).values
    } yield (grouped, controls)
  }
}
