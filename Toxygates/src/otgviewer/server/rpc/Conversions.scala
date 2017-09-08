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

import otg.Context
import otg.OTGSeries
import otgviewer.shared.Pathology
import otgviewer.shared.RankRule
import otgviewer.shared.Series
import t.SeriesRanking
import t.common.shared.sample._
import t.db.MatrixContext
import otgviewer.shared.RuleType
import t.model.sample.CoreParameter._
import otg.model.sample.OTGAttribute._

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

	new OTGSeries(sc.get(Repeat),
	    sc.get(Organ), sc.get(Organism),
	    p, sc.get(Compound), sc.get(DoseLevel), sc.get(TestType), Vector())
  }

  implicit def asJava(series: OTGSeries)(implicit context: Context): Series = {
    implicit val mc = context.matrix
    val name = series.compound + " " + series.dose
    val sc = new t.model.SampleClass
    sc.put(DoseLevel, series.dose)
    sc.put(Compound, series.compound)
    sc.put(Organism, series.organism)
    sc.put(TestType, series.testType)
    sc.put(Organ, series.organ)
    sc.put(Repeat, series.repeat)
    new Series(name, series.probeStr, ExposureTime.id(), sc,
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
