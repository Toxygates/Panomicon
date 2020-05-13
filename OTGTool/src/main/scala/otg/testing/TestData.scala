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

package otg.testing

import otg.DoseSeries
import otg.OTGSeries
import otg.TimeSeries
import t.db.Metadata
import t.model.sample.OTGAttribute._
import t.Factory
import t.db._
import t.model.sample
import t.model.sample.{Attribute, OTGAttributeSet}
import t.model.shared.SampleClassHelper._
import t.platform._

object TestData {
  import t.db.testing.TestData._

  def mkPoint(pr: String, t: Int) = {
     val e = randomExpr()
     SeriesPoint(t, new BasicExprValue(e._1, e._2, pr))
  }

  val absentTime = "9 hr"
  val absentDose = "Middle"

  val usedTimePoints = enumMaps(ExposureTime.id).filter(_._1 != absentTime)
  val usedDosePoints = DoseSeries.allDoses.filter(_ != absentDose).map(p =>
    (p, enumMaps(DoseLevel.id)(p)))

  def mkPointsTime(pr: String): Seq[SeriesPoint] = {
    usedTimePoints.map(t => mkPoint(pr, t._2)).toSeq
  }

  def mkPointsDose(pr: String): Seq[SeriesPoint] = {
    usedDosePoints.map(t => mkPoint(pr, t._2)).toSeq
  }

  lazy val series = for (compound <- enumValues(Compound.id).toSeq;
    doseLevel <- usedDosePoints.map(_._1);
    repeat <- enumValues(Repeat.id);
    organ <- enumValues(Organ.id);
    organism <- enumValues(Organism.id);
    testType <- enumValues(TestType.id);
    probe <- probes;
    points = mkPointsTime(probeMap.unpack(probe))
    ) yield OTGSeries(TimeSeries, repeat, organ, organism, probe,
        compound, doseLevel, testType, points)

  lazy val doseSeries = for (compound <- enumValues(Compound.id).toSeq;
    exposureTime <- usedTimePoints.map(_._1);
    repeat <- enumValues(Repeat.id);
    organ <- enumValues(Organ.id);
    organism <- enumValues(Organism.id);
    testType <- enumValues(TestType.id);
    probe <- probes;
    points = mkPointsDose(probeMap.unpack(probe))
    ) yield OTGSeries(DoseSeries, repeat, organ, organism, probe,
        compound, exposureTime, testType, points)

  private def controlGroup(s: Sample) = ???

  import t.model.sample.CoreParameter.{ ControlGroup => CGParam }

  lazy val controlGroups: Map[Sample, SSVarianceSet] = {
    val gr = samples.groupBy(_(CGParam))
    val controls = gr.mapValues(vs =>
      new SSVarianceSet(metadata,
          vs.toSeq.filter(_(DoseLevel) == "Control"))
      )

    Map() ++ samples.map(s => s -> controls(s(CGParam)))
  }

  //temporary
  val attribSet = OTGAttributeSet.getDefault

  val bioParams = Seq(
      BioParameter(LiverWeight, None, None, None),
      BioParameter(KidneyWeight, None, None, None)
        )

  val bioParameters = new BioParameters(Map() ++ bioParams.map(p => p.attribute -> p))

  def randomNumber(mean: Double, range: Double) =
    Math.random * range + (mean - range/2)

  def liverWeight(s: Sample) =
    t.db.testing.TestData.liverWeight(s(DoseLevel), s(Individual))

  def kidneyWeight(s: Sample) =
    t.db.testing.TestData.kidneyWeight(s(DoseLevel), s(Individual))

  def metadata: Metadata = new Metadata {
    def samples = t.db.testing.TestData.samples

    def mapParameter(fact: Factory, key: String, f: String => String) = ???

    def attributeValues(attribute: Attribute): Seq[String] =
      enumMaps(attribute.id).keys.toSeq.distinct

    def attributeSet = sample.OTGAttributeSet.getDefault

    def sampleAttributes(s: Sample): Seq[(Attribute, String)] = {
      samples.find(_ == s).get.sampleClass.asScalaMap.toSeq ++ Seq(
          (LiverWeight, "" + liverWeight(s)),
          (KidneyWeight, "" + kidneyWeight(s))
          )
    }
  }
}
