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

package otg

import t.platform.Species._
import otg.model.sample.OTGAttribute._
import t.db._
import t.db.{ Series => TSeries }
import t.model.sample.Attribute
import scala.reflect.ClassTag

/**
 * A series of points (either expression vs time, or expression vs dose).
 * If the series is partially specified (some parameters missing),
 * it can be used to match on or specify a range in the space of all series.
 *
 * The following parameters are nullable: repeat, organ, organism,
 * probe, compound, doseOrTime, testType
 */
case class OTGSeries(seriesType: OTGSeriesType, repeat: String, organ: String, organism: String,
    override val probe: Int, compound: String, doseOrTime: String, testType: String,
  override val points: Seq[SeriesPoint] = Seq()) extends TSeries[OTGSeries](probe, points) {

  def classCode(implicit mc: MatrixContext): Long = seriesType.builder.pack(this)

  def asSingleProbeKey = copy(probe = probe, compound = null, doseOrTime = null)

  /**
   * Obtain the attribute constraints encoded by this series or
   * partially specified series
   */
  override def constraints: Map[Attribute, String] = {
    val map: Map[Attribute, String] = Map(TestType -> testType,
       Organ -> organ,
       Organism -> organism,
       Compound -> compound,
       seriesType.lastConstraint -> doseOrTime,
       Repeat -> repeat
       )
    map.filter(_._2 != null)
  }
}

trait OTGSeriesType {
  def lastConstraint: Attribute
  def independentVariable: Attribute
  def independentVariableMap(implicit mc: MatrixContext) = mc.enumMaps(independentVariable)
  def expectedIndependentVariablePoints(key: OTGSeries): Seq[String]
  def standardEnumValues: Iterable[(String, String)]
  def builder: OTGSeriesBuilder
}

object DoseSeries extends OTGSeriesType {
  val lastConstraint = ExposureTime
  val independentVariable = DoseLevel

  val allDoses = List("Low", "Middle", "High")
  val standardEnumValues = allDoses.map(x => (DoseLevel.id, x))
  def builder = OTGDoseSeriesBuilder

  def expectedIndependentVariablePoints(key: OTGSeries): Seq[String] = allDoses
}

object OTGDoseSeriesBuilder extends OTGSeriesBuilder(DoseSeries)

object TimeSeries extends OTGSeriesType {
  val lastConstraint = DoseLevel
  val independentVariable = ExposureTime
  def builder = OTGTimeSeriesBuilder

  val vitroExpected = Vector("2 hr", "8 hr", "24 hr")
  val singleVivoExpected = Vector("3 hr", "6 hr", "9 hr", "24 hr")
  val repeatVivoExpected = Vector("4 day", "8 day", "15 day", "29 day")
  def allExpectedTimes = vitroExpected ++ singleVivoExpected ++ repeatVivoExpected

  val standardEnumValues = allExpectedTimes.map(x => (ExposureTime.id, x))

  def expectedIndependentVariablePoints(key: OTGSeries): Seq[String] = {
    if (key.testType == null) {
      throw new Exception("Test type must be specified")
    } else if (key.testType == "in vitro") {
      vitroExpected
    } else {
      if (key.repeat == null) {
        throw new Exception("Repeat type must be specified")
      } else if (key.repeat == "Single") {
        singleVivoExpected
      } else {
        repeatVivoExpected
      }
    }
  }
}

object OTGTimeSeriesBuilder extends OTGSeriesBuilder(TimeSeries)

class OTGSeriesBuilder(val seriesType: OTGSeriesType) extends SeriesBuilder[OTGSeries] {

  val enums = List(Repeat, Organ, Organism, DoseLevel, ExposureTime,
    Compound, TestType).map(_.id)

  private def rem(mc: MatrixContext, key: String): Map[Int, String] =
    mc.reverseEnumMaps(key)
  private def rem(mc: MatrixContext, key: Attribute): Map[Int, String] =
    rem(mc, key.id)

  def build(sampleClass: Long, probe: Int)(implicit mc: MatrixContext): OTGSeries = {
    val compound = rem(mc, Compound)(((sampleClass) & 65535).toInt)
    val doseOrTime = rem(mc, seriesType.lastConstraint)(((sampleClass >> 16) & 255).toInt)
    val organism = rem(mc, Organism)(((sampleClass >> 24) & 255).toInt)
    val organ = rem(mc, Organ)(((sampleClass >> 32) & 255).toInt)
    val repeat = rem(mc, Repeat)(((sampleClass >> 40) & 3).toInt)
    val test = rem(mc, TestType)(((sampleClass >> 42) & 3).toInt)

    OTGSeries(seriesType, repeat, organ, organism, probe, compound, doseOrTime, test, Vector())
  }

  def pack(s: OTGSeries)(implicit mc: MatrixContext): Long = {
    var r = 0l
    r |= packWithLimit(TestType, s.testType, 3) << 42
    r |= packWithLimit(Repeat, s.repeat, 3) << 40
    r |= packWithLimit(Organ, s.organ, 255) << 32
    r |= packWithLimit(Organism, s.organism, 255) << 24
    r |= packWithLimit(seriesType.lastConstraint, s.doseOrTime, 255) << 16
    r |= packWithLimit(Compound, s.compound, 65535)
    r
  }

  def rebuild(from: OTGSeries, points: Iterable[SeriesPoint]): OTGSeries = {
    from.copy(points = points.toVector)
  }

  def keysFor(group: OTGSeries)(implicit mc: MatrixContext): Iterable[OTGSeries] = {
    def singleOrKeys(v: String, attrib: Attribute) =
      if (v != null) List(v) else mc.enumMaps(attrib.id).keys

    val rs = singleOrKeys(group.repeat, Repeat)
    val os = singleOrKeys(group.organ, Organ)
    val ss = singleOrKeys(group.organism, Organism)
    val cs = singleOrKeys(group.compound, Compound)
    val ds = singleOrKeys(group.doseOrTime, seriesType.lastConstraint)
    val ts = singleOrKeys(group.testType, TestType)

    val empty = Vector()

    val r = for (r <- rs; o <- os; s <- ss; c <- cs; d <- ds; t <- ts)
      yield OTGSeries(seriesType, r, o, s, group.probe, c, d, t, empty)
    //println("Generated keys:" + r.mkString("\n\t"))
    r
  }

  def buildEmpty(x: Sample, md: Metadata) = {
    val attribs = Map() ++ md.sampleAttributes(x)

    OTGSeries(seriesType, attribs(Repeat), attribs(Organ), attribs(Organism), 0,
      attribs(Compound), attribs(seriesType.lastConstraint), attribs(TestType), Vector())
  }

  /**
   * Group samples that belong to the same series together.
   */
  def groupSamples(xs: Iterable[Sample], md: Metadata): Iterable[(OTGSeries, Iterable[Sample])] =
    xs.groupBy(buildEmpty(_, md))

  def makeNew[E >: Null <: ExprValue : ClassTag](from: MatrixDBReader[E], md: Metadata,
      samples: Iterable[Sample])(implicit mc: MatrixContext): Iterable[OTGSeries] = {

    val timeOrDoseMap = seriesType.independentVariableMap

    val grouped = groupSamples(samples, md)
    var r = Vector[OTGSeries]()

    for ((series, xs) <- grouped) {
      //Construct the series s for all probes, using the samples xs

      val repSample = xs.head
      val probes = from.sortProbes(mc.expectedProbes(repSample))
      val indepPoints = xs.groupBy(x => md.sampleAttribute(x, seriesType.independentVariable).get)

      val spoints = (for (
        (point, pointSamples) <- indepPoints.toSeq.par;
        samples = from.sortSamples(pointSamples);
        exprs = from.valuesForSamplesAndProbes(samples, probes, false, false);
        (pr, data) <- (probes zip exprs);
        nonPadded = data.filter(!_.isPadding);
        if (!nonPadded.isEmpty);
        mean = meanPoint(nonPadded);
        spoint = SeriesPoint(timeOrDoseMap(point), mean))
        yield (pr, spoint))

      for ((pr, points) <- spoints.seq.groupBy(_._1)) {
        r :+= series.copy(probe = pr, points = points.map(_._2))
      }
    }
    println(s"Constructed ${r.size} series including: ${r.head}")
    r
  }

  def standardEnumValues = seriesType.standardEnumValues

  def expectedIndependentVariablePoints(key: OTGSeries) = seriesType.expectedIndependentVariablePoints(key)

  /**
   * Normalize independent variable points.
   * (Necessary because a different number might be available for
   * different compounds, for example).
   * The input series must have the same repeat type and organ.
   */
  def normalize(data: Iterable[OTGSeries])(implicit mc: MatrixContext): Iterable[OTGSeries] = {
    if (data.isEmpty) {
      return data
    }
    val independentVariablePoints = expectedIndependentVariablePoints(data.head)
    val indepnedentVariableMap = seriesType.independentVariableMap
    val independentVariableCodes = independentVariablePoints.map(indepnedentVariableMap) //sorted
    val absentValue = BasicExprValue(0, 'A')

    data.map(s => {
      rebuild(s,
        independentVariableCodes.map(c => s.points.find(_.code == c).getOrElse(SeriesPoint(c, absentValue))))
    })
  }
}
