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

package otg

import scala.annotation.tailrec
import friedrich.util.CmdLineOptions
import t.db.MatrixDB
import t.db.SeriesDB
import t.db.kyotocabinet.KCMatrixDB
import otg.sparql.OTGSamples
import otg.Species._
import t.db.MatrixDBReader
import t.db.kyotocabinet.KCSeriesDB
import t.db.{ Series => TSeries }
import t.db.MatrixContext
import t.db.SeriesBuilder
import t.db.SeriesPoint
import t.db.Sample
import t.db.Metadata
import t.db.ExprValue
import t.db.BasicExprValue

//TODO all parameters are nullable - use options
case class OTGSeries(repeat: String, organ: String, organism: String, override val probe: Int,
  compound: String, dose: String, testType: String,
  override val points: Seq[SeriesPoint] = Seq()) extends TSeries[OTGSeries](probe, points) {

  def classCode(implicit mc: MatrixContext): Long =
    OTGSeries.pack(this)

  def asSingleProbeKey = copy(probe = probe, compound = null, dose = null)

  override def constraints: Map[String, String] = Map(
       "test_type" -> testType,
       "organ_id" -> organ,
       "organism" -> organism,
       "compound_name" -> compound,
       "dose_level" -> dose,
       "sin_rep_type" -> repeat
       ).filter(_._2 != null)
}

object OTGSeries extends SeriesBuilder[OTGSeries] {
  val enums = List("sin_rep_type", "organ_id", "organism",
    "dose_level", "exposure_time", "compound_name", "test_type")

  def build(sampleClass: Long, probe: Int)(implicit mc: MatrixContext): OTGSeries = {
    val compound = mc.reverseEnumMaps("compound_name")(((sampleClass) & 65535).toInt)
    val dose = mc.reverseEnumMaps("dose_level")(((sampleClass >> 16) & 255).toInt)
    val organism = mc.reverseEnumMaps("organism")(((sampleClass >> 24) & 255).toInt)
    val organ = mc.reverseEnumMaps("organ_id")(((sampleClass >> 32) & 255).toInt)
    val repeat = mc.reverseEnumMaps("sin_rep_type")(((sampleClass >> 40) & 3).toInt)
    val test = mc.reverseEnumMaps("test_type")(((sampleClass >> 42) & 3).toInt)

    OTGSeries(repeat, organ, organism, probe, compound, dose, test, Vector())
  }

  def pack(s: OTGSeries)(implicit mc: MatrixContext): Long = {
    var r = 0l
    r |= packWithLimit("test_type", s.testType, 3) << 42
    r |= packWithLimit("sin_rep_type", s.repeat, 3) << 40
    r |= packWithLimit("organ_id", s.organ, 255) << 32
    r |= packWithLimit("organism", s.organism, 255) << 24
    r |= packWithLimit("dose_level", s.dose, 255) << 16
    r |= packWithLimit("compound_name", s.compound, 65535)
    r
  }

  def rebuild(from: OTGSeries, points: Iterable[SeriesPoint]): OTGSeries = {
    from.copy(points = points.toVector)
  }

  def keysFor(group: OTGSeries)(implicit mc: MatrixContext): Iterable[OTGSeries] = {
    def singleOrKeys(v: String, enum: String) =
      if (v != null) List(v) else mc.enumMaps(enum).keys

    val rs = singleOrKeys(group.repeat, "sin_rep_type")
    val os = singleOrKeys(group.organ, "organ_id")
    val ss = singleOrKeys(group.organism, "organism")
    val cs = singleOrKeys(group.compound, "compound_name")
    val ds = singleOrKeys(group.dose, "dose_level")
    val ts = singleOrKeys(group.testType, "test_type")

    val empty = Vector()

    val r = for (r <- rs; o <- os; s <- ss; c <- cs; d <- ds; t <- ts)
      yield OTGSeries(r, o, s, group.probe, c, d, t, empty)
    println("Generated keys:" + r.mkString("\n\t"))
    r
  }

  def buildEmpty(x: Sample, md: Metadata) = {
    val paramMap = Map() ++ md.parameters(x).map(x => x._1.identifier -> x._2)
        val r = paramMap("sin_rep_type")
        val d = paramMap("dose_level")
        val o = paramMap("organ_id")
        val s = paramMap("organism")
        val c = paramMap("compound_name")
        val t = paramMap("test_type")
        OTGSeries(r, o, s, 0, c, d, t, Vector())
  }

  def makeNew[E >: Null <: ExprValue](from: MatrixDBReader[E], md: Metadata,
      samples: Iterable[Sample])(implicit mc: MatrixContext): Iterable[OTGSeries] = {

    val timeMap = mc.enumMaps("exposure_time")

    val grouped = samples.groupBy(buildEmpty(_, md))
    var r = Vector[OTGSeries]()

    for ((s, xs) <- grouped) {
      //Construct the series s for all probes, using the samples xs

      val data = for (
        x <- xs;
        exprs = from.valuesInSample(x, Seq());
        presentExprs = exprs.filter(_.present);
        time = md.parameter(x, "exposure_time")
      ) yield (time, x, presentExprs)


      val byTime = for (
        (time, data) <- data.groupBy(_._1);
        tc = timeMap(time);
        meanValues = presentMeanByProbe(data.flatMap(_._3));
        m <- meanValues;
        point = SeriesPoint(tc, m)
      ) yield point


      val byProbe = byTime.groupBy(_.value.probe)
      r ++= byProbe.map(x => {
        s.copy(probe = mc.probeMap.pack(x._1), points = x._2.toSeq)
      })
    }
    r
  }

  private def isBefore(time1: String, time2: String): Boolean = {
    val units = List("min", "hr", "day", "week", "month")

    def split(t: String) = {
      val s = t.split(" ")
      if (s.length != 2) {
        throw new Exception("Invalid time format: " + t + " (example: 9 hr)")
      }
      if (!units.contains(s(1))) {
        throw new Exception("Invalid time unit: " + s(1) +
          " (valid: " + units.mkString(" ") + ")")
      }
      (s(0).toInt, s(1))
    }

    val (q1, u1) = split(time1)
    val (q2, u2) = split(time2)
    if (units.indexOf(u2) > units.indexOf(u1)) {
      true
    } else if (units.indexOf(u1) > units.indexOf(u2)) {
      false
    } else {
      q1 < q2
    }
  }

  def sortTimes(items: Iterable[String]): Seq[String] =
    items.toList.sortWith(isBefore)

  //TODO we don't currently use dose series
  val expectedDoses = List("Low", "Middle", "High")

  def expectedTimes(key: OTGSeries): Seq[String] = {
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

  val vitroExpected = Vector("2 hr", "8 hr", "24 hr")
  val singleVivoExpected = Vector("3 hr", "6 hr", "9 hr", "24 hr")
  val repeatVivoExpected = Vector("4 day", "8 day", "15 day", "29 day")

  def allExpectedTimes = vitroExpected ++ singleVivoExpected ++ repeatVivoExpected

  val standardEnumValues = allExpectedTimes.map(x => ("exposure_time", x))

  /**
   * Normalize time points.
   * (Necessary because a different number might be available for
   * different compounds, for example).
   * The input series must have the same repeat type and organ.
   */
  def normalize(data: Iterable[OTGSeries])(implicit mc: MatrixContext): Iterable[OTGSeries] = {
    if (data.isEmpty) {
      return data
    }
    val times = expectedTimes(data.head)
    val timeMap = mc.enumMaps("exposure_time")
    val timeCodes = times.map(timeMap) //sorted
    val absentValue = BasicExprValue(0, 'A')

    data.map(s => {
      rebuild(s,
        timeCodes.map(c => s.points.find(_.code == c).getOrElse(SeriesPoint(c, absentValue))))
    })
  }
}
