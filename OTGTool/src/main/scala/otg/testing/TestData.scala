/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package otg.testing

import scala.collection.JavaConversions._

import otg.OTGSeries
import otg.db.Metadata
import otg.model.sample.OTGAttribute._
import t.Factory
import t.db._
import t.db.testing.TestData.enumMaps
import t.model.sample.Attribute
import t.platform._

object TestData {
  import t.db.testing.TestData._

  def mkPoint(pr: String, t: Int) = {
     val e = randomExpr()
     SeriesPoint(t, new BasicExprValue(e._1, e._2, pr))
  }

  def mkPoints(pr: String): Seq[SeriesPoint] = {
    val indepPoints = enumMaps(ExposureTime.id).filter(_._1 != "9 hr").map(_._2)
    indepPoints.map(t => mkPoint(pr, t)).toSeq
  }

  lazy val series = for (compound <- enumValues(Compound.id);
    doseLevel <- enumValues(DoseLevel.id);
    repeat <- enumValues(Repeat.id);
    organ <- enumValues(Organ.id);
    organism <- enumValues(Organism.id);
    testType <- enumValues(TestType.id);
    probe <- probes;
    points = mkPoints(probeMap.unpack(probe))
    ) yield OTGSeries(repeat, organ, organism, probe,
        compound, doseLevel, testType, points)

  private def controlGroup(s: Sample) = ???

  import t.model.sample.CoreParameter.{ControlGroup => CGParam}

  lazy val controlGroups: Map[Sample, SSVarianceSet] = {
    val gr = samples.groupBy(_(CGParam))
    val controls = gr.mapValues(vs =>
      new SSVarianceSet(metadata,
          vs.toSeq.filter(_(DoseLevel) == "Control"))
      )

    Map() ++ samples.map(s => s -> controls(s(CGParam)))
  }

  //temporary
  val attribSet = otg.model.sample.AttributeSet.getDefault

  val bioParams = Seq(
      BioParameter(LiverWeight, None, None, None),
      BioParameter(KidneyWeight, None, None, None)
        )

  val bioParameters = new BioParameters(Map() ++ bioParams.map(p => p.attribute -> p))

  def randomNumber(mean: Double, range: Double) =
    Math.random * range + (mean - range/2)

  def liverWt(s: Sample) =
    if (s(DoseLevel) == "Control" || s(Individual) == "2")
      //TODO find a better way to generate values with predictable s.d.
      3 //healthy
    else
      randomNumber(5, 0.2) //abnormal individual_id 1, 3

  def kidneyWt(s: Sample) =
    if (s(DoseLevel) == "Control" || s(Individual) == "1")
      //TODO find a better way to generate values with predictable s.d.
      5 //healthy
    else
      randomNumber(1, 0.2) //abnormal individual_id 2, 3

  def metadata: Metadata = new Metadata {
    def samples = t.db.testing.TestData.samples

    def mapParameter(fact: Factory, key: String, f: String => String) = ???

    def parameterValues(identifier: String): Set[String] =
      enumMaps(identifier).keySet

    def attributes = otg.model.sample.AttributeSet.getDefault

    def parameters(s: Sample): Seq[(Attribute, String)] = {
      samples.find(_ == s).get.sampleClass.getMap.map(x =>  {
         val k = attribSet.byId(x._1)
         (k, x._2)
      }).toSeq ++ Seq(
          (LiverWeight, "" + liverWt(s)),
          (KidneyWeight, "" + kidneyWt(s))
          )
    }
  }
}
