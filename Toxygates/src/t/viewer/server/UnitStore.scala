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

package t.viewer.server

import t.common.shared.DataSchema
import t.common.shared.Pair
import t.common.shared.sample.Sample
import t.common.shared.sample.SampleClassUtils
import t.common.shared.sample.Unit
import t.db.SimpleVarianceSet
import t.model.SampleClass
import t.model.sample.CoreParameter._
import t.model.sample.VarianceSet
import t.sparql.SampleClassFilter
import t.sparql.SampleFilter
import t.sparql.SampleStore
import t.viewer.server.Conversions._
import t.viewer.shared.TimeoutException

class UnitStore(schema: DataSchema, sampleStore: SampleStore) extends
  UnitsHelper(schema) {
  /**
   * Generates units containing treated samples and their associated control samples.
   * Task: sensitive algorithm, should simplify and possibly move to OTGTool.
   */
  @throws[TimeoutException]
  def units(sc: SampleClass, param: String, paramValues: Array[String],
            sf: SampleFilter) : Array[Pair[Unit, Unit]] = {

    //This will filter by the chosen parameter - usually compound name
    val rs = sampleStore.samples(SampleClassFilter(sc), param,
      paramValues.toSeq, sf)
    units(sc, rs, sf).map(x => new Pair(x._1, x._2.getOrElse(null))).toArray
  }

  def units(sc: SampleClass, samples: Iterable[t.db.Sample],
            sf: SampleFilter): Iterable[(Unit, Option[Unit])] = {
    def isControl(s: t.db.Sample) = schema.isSelectionControl(s.sampleClass)

    def unit(s: Sample) = SampleClassUtils.asUnit(s.sampleClass, schema)

    //Note: the copying may be costly - consider optimising in the future
    def unitWithoutMajorMedium(s: Sample) = unit(s).
      copyWithout(schema.majorParameter).copyWithout(schema.mediumParameter)

    def asUnit(ss: Iterable[Sample]) = new Unit(unit(ss.head), ss.toArray)

    val groupedSamples = samples.groupBy(x =>(
            x.sampleClass(Batch),
            x.sampleClass(ControlGroup)))

    val cgs = groupedSamples.keys.toSeq.map(_._2).distinct
    val potentialControls = sampleStore.samples(SampleClassFilter(sc),
      ControlGroup.id, cgs, sf).filter(isControl).map(asJavaSample)

      /*
       * For each unit of treated samples inside a control group, all
       * control samples in that group are assigned as control,
       * assuming that other parameters are also compatible.
       */

    var r = Vector[(Unit, Option[Unit])]()
    for (((batch, cg), samples) <- groupedSamples;
        treated = samples.filter(!isControl(_)).map(asJavaSample)) {

      /*
       * Remove major parameter (compound in OTG case) as we now allow treated-control samples
       * to have different compound names.
       */

      val treatedByUnit = treated.groupBy(unit(_))

      val treatedControl = treatedByUnit.toSeq.map(unitSamples => {
        val repSample = unitSamples._2.head
        val repUnit = unitWithoutMajorMedium(repSample)

        val filteredControls = potentialControls.filter(s =>
          unitWithoutMajorMedium(s) == repUnit
          && s.get(ControlGroup) == repSample.get(ControlGroup)
          && s.get(Batch) == repSample.get(Batch)
          )

        val controlUnit = if (filteredControls.isEmpty)
          None
        else
          Some(asUnit(filteredControls))

        val treatedUnit = asUnit(unitSamples._2)

        treatedUnit -> controlUnit
      })

      r ++= treatedControl

      for (
        (treat, Some(control)) <- treatedControl;
        samples = control.getSamples;
        if (!samples.isEmpty); // this check shouldn't be necessary
        pseudoUnit = (control, None)
      ) {
        r :+= pseudoUnit
      }
    }
    r
  }
}

class UnitsHelper(schema: DataSchema) {

  def byControlGroup(raw: Iterable[Unit]): Map[String, Iterable[Unit]] =
    raw.groupBy(controlGroupKey)

  def controlGroupKey(u: Unit): String = controlGroupKey(u.getSamples()(0))

  def samplesByControlGroup(raw: Iterable[Sample]): Map[String, Iterable[Sample]] =
    raw.groupBy(controlGroupKey)

  def controlGroupKey(s: Sample): String = s.get(ControlTreatment)

  def unitGroupKey(s: Sample) = s.get(schema.mediumParameter())

  /**
   * Groups the samples provided into treated and control units, returning
   * a list of tuples whose first element is a treated unit and whose second
   * element is a tuple containing the corresponding control unit and variance
   * set.
   * @param samples samples to partition
   */
  def formControlUnitsAndVarianceSets(samples: Iterable[Sample]):
      Seq[(Unit, (Unit, VarianceSet))] = {
    formTreatedAndControlUnits(samples).flatMap {
      case (treatedSamples, controlSamples) =>
        if (treatedSamples.nonEmpty && controlSamples.nonEmpty) {
          val units = treatedSamples.map(formUnit(_, schema))
          val controlGroup = formUnit(controlSamples, schema)
          val varianceSet = new SimpleVarianceSet(controlSamples)
          units.map(_ -> (controlGroup, varianceSet))
        } else {
          Seq.empty
        }
    }
  }

  /**
   * Groups the samples provided into treated and control groups, returning
   * a list of tuples whose first element is the samples in a treated unit and
   * whose second element is the samples from the corresponding control unit.
   * @param samples samples to partition
   */
  def formTreatedAndControlUnits(samples: Iterable[Sample]):
      Seq[(Iterable[Iterable[Sample]], Iterable[Sample])] = {
    val controlGroups = samples.groupBy(controlGroupKey)
    controlGroups.map { case (unitKey, samples) =>
      samples.partition(!schema.isControl(_)) match {
        case (treated, controls) =>
          (treated.groupBy(unitGroupKey).values, controls)
      }
    }.toList
  }

  /**
   * Forms a unit from a set of samples.
   * @param samples all of the samples from the desired unit
   */
  def formUnit(samples: Iterable[Sample], schema:DataSchema): Unit = {
    new Unit(SampleClassUtils.asUnit(samples.head.sampleClass(), schema),
        samples.toArray)
  }
}
