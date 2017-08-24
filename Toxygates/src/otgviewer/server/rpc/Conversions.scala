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

package otgviewer.server.rpc

import scala.collection.JavaConversions._
import scala.language.implicitConversions

import t.SeriesRanking
import t.platform.Species
import t.platform.Species._
import otgviewer.shared.Pathology
import otgviewer.shared.RankRule
import otgviewer.shared.Series
import t.common.shared.sample._
import otg.SeriesRanking
import otg.Context
import otgviewer.shared.RuleType
import otg.OTGSeries
import t.common.shared.Pair
import t.common.shared.sample.Sample

import t.db.MatrixContext
import t.common.shared.FirstKeyedPair

/**
 * Conversions between Scala and Java types.
 * In many cases these can be used as implicits.
 * The main reason why this is sometimes needed is that for RPC,
 * GWT can only serialise Java classes that follow certain constraints.
 */
object Conversions {
  import t.viewer.server.Conversions._

  implicit def asJava(path: otg.Pathology): Pathology =
    new Pathology(path.barcode, path.topography.getOrElse(null),
        path.finding.getOrElse(null),
        path.spontaneous, path.grade.getOrElse(null), path.digitalViewerLink);

  implicit def asScala(series: Series)(implicit context: MatrixContext): OTGSeries = {
	val p = context.probeMap.pack(series.probe) //TODO filtering
	val sc = series.sampleClass

	new OTGSeries(sc.get("sin_rep_type"),
	    sc.get("organ_id"), sc.get("organism"),
	    p, sc.get("compound_name"), sc.get("dose_level"), sc.get("test_type"), Vector())
  }

  implicit def asJava(series: OTGSeries)(implicit context: Context): Series = {
    implicit val mc = context.matrix
    val name = series.compound + " " + series.dose
    val sc = new t.model.SampleClass
    sc.put("dose_level", series.dose)
    sc.put("compound_name", series.compound)
    sc.put("organism", series.organism)
    sc.put("test_type", series.testType)
    sc.put("organ_id", series.organ)
    sc.put("sin_rep_type", series.repeat)
    new Series(name, series.probeStr, "exposure_time", sc,
         series.values.map(t.viewer.server.Conversions.asJava).toArray)
  }

  implicit def asScala(rr: RankRule): SeriesRanking.RankType = {
    rr.`type`() match {
      case s: RuleType.Synthetic.type  => {
        println("Correlation curve: " + rr.data.toVector)
        SeriesRanking.MultiSynthetic(rr.data.toVector)
      }
      case _: RuleType.HighVariance.type => SeriesRanking.HighVariance
      case _: RuleType.LowVariance.type => SeriesRanking.LowVariance
      case _: RuleType.Sum.type => SeriesRanking.Sum
      case _: RuleType.NegativeSum.type => SeriesRanking.NegativeSum
      case _: RuleType.Unchanged.type => SeriesRanking.Unchanged
      case _: RuleType.MonotonicUp.type => SeriesRanking.MonotonicIncreasing
      case _: RuleType.MonotonicDown.type => SeriesRanking.MonotonicDecreasing
      case _: RuleType.MaximalFold.type => SeriesRanking.MaxFold
      case _: RuleType.MinimalFold.type => SeriesRanking.MinFold
      case _: RuleType.ReferenceCompound.type => SeriesRanking.ReferenceCompound(rr.compound, rr.dose)
    }
  }
}
