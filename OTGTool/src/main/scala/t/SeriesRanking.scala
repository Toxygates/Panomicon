/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t

import t.util.SafeMath
import t.db._
import friedrich.data.Statistics

abstract class SeriesRanking[S <: Series[S]](val db: SeriesDB[S], val key: S)(implicit context: MatrixContext) {
  protected def getScores(mt: SeriesRanking.RankType): Iterable[(S, Double)] = {
    mt.matchOneProbe(this, key.probe)
  }

  /**
   * This method is currently the only entry point used by the web application.
   * Returns (compound, dose, score)
   */
  def rankCompoundsCombined(probesRules: Seq[(String, SeriesRanking.RankType)]): Iterable[(String, String, Double)] 

}

/*
 * Note: some of the rules might not be deterministic (in the case where only a
 * partial ordering is imposed by the scoring function). 
 */
object SeriesRanking {
  import Statistics._
  import SafeMath._

  // The different rank types (scoring methods).
  // Note that each rank type must produce a POSITIVE number to function correctly.

  trait RankType {
    /**
     * Perform ranking for one probe.
     * Note that the probe in the pattern 'key' may or may not correspond to the probe being ranked.
     * The returned values are tuples of the compound names and matching scores for the various doses/times.
     */
    def matchOneProbe[S <: Series[S]](c: SeriesRanking[S], probe: Int): Iterable[(S, Double)] = {
      val key = c.key.asSingleProbeKey
      val data = c.db.read(key)
      println(s"Read for key: $key result size ${data.size}")

      scoreAllSeries(data.toSeq)
    }

    def scoreAllSeries[S <: Series[S]](series: Seq[S]): Iterable[(S, Double)] = {
      series.map(s => (s, scoreSeries(s)))
    }

    def scoreSeries[S <: Series[S]](s: S): Double
  }

  case class MultiSynthetic(pattern: Vector[Double]) extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double =
      safePCorrelation(pattern, s.values.toVector.map(_.value)) + 1
  }

  object Sum extends RankType {
    // TODO: numbers like 50 are ad hoc to guarantee a positive result
    def scoreSeries[S <: Series[S]](s: S): Double =
      (50 + safeMean(s.presentValues)) / 50
  }

  object NegativeSum extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double =
      (50 - safeMean(s.presentValues)) / 50
  }

  object Unchanged extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double =
      (1000 - safeSum(s.presentValues.map(x => x * x))) / 1000
  }

  /**
   * This match type maximises standard deviation.
   */
  object HighVariance extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double =
      (10 + safeSigma(s.presentValues)) / 10
  }

  /**
   * This match type minimises standard deviation.
   */
  object LowVariance extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double =
      (10 - safeSigma(s.presentValues)) / 10
  }

  object MonotonicIncreasing extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double = {
      var score = 5
      if (s.values(0).value < 0 || s.values(0).call == 'A') { //optional: remove this constraint
        score -= 1
      }
      for (i <- 0 until s.points.size - 1) {
        if (s.values(i + 1).value < s.values(i).value - 0.001 ||
          s.values(i + 1).value < 0.001
          || s.values(i + 1).call == 'A') {
          score -= 1
        }
      }
      
      /*
       * We penalise missing data heavily. This may be worth reconsidering.
       */
      //score -= (4 - s.values.size) 
      score
    }
  }

  object MonotonicDecreasing extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double = {
      var score = 5
      if (s.values(0).value > 0 || s.values(0).call == 'A') { //optional: remove this constraint
        score -= 1
      }
      for (i <- 0 until s.values.size - 1) {
        if (s.values(i + 1).value > s.values(i).value + 0.001 ||
          s.values(i + 1).value > 0.001
          || s.values(i + 1).call == 'A') {
          score -= 1
        }
      }
      //score -= (4 - s.values.size) //penalise missing data heavily
      score
    }
  }

  object MinFold extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double = 20 - safeMin(s.presentValues)
  }

  object MaxFold extends RankType {
    def scoreSeries[S <: Series[S]](s: S): Double = 20 + safeMax(s.presentValues)
  }

  case class ReferenceCompound[S <: Series[S]](compound: String, doseOrTime: String) extends RankType {
    var refCurves: Iterable[S] = Iterable.empty
    def init(db: SeriesDB[S], key: S) {
      refCurves = db.read(key)
      println("Initialised " + refCurves.size + " reference curves")
      println(refCurves.mkString)
    }

    def scoreSeries[T <: Series[T]](s: T): Double =
      safeMax(refCurves.map(rc => safePCorrelation(rc, s) + 1))
  }

  def safePCorrelation(s1: Vector[Double], s2: Vector[Double]): Double = {
    if (s1.size == s2.size) {
      if (s1.size < 2) {
        // Don't even try in this case.
        println("Too few values - unable to rank")
        Double.NaN
      } else {
        pearsonCorrelation(0.0 +: s1, 0.0 +: s2)
      }
    } else {
      //We don't expect this but let's handle it nicely.
      println("Warning: attempt to do safePCorrelation for vectors of different length.")
      println(s"s1: $s1 \n s2: $s2")
      Double.NaN
    }
  }

  def safePCorrelation(s1: Series[_], s2: Series[_]): Double = {
    val (mut1, mut2) = mutualPoints(s1, s2)
    safePCorrelation(mut1.map(_.value).toVector, mut2.map(_.value).toVector)
  }

  def mutualPoints(s1: Series[_], s2: Series[_]): (Seq[ExprValue], Seq[ExprValue]) = {
    val (d1, d2) = (s1.values, s2.values)
    // pick the matching points to perform ranking
    if (d1.size != d2.size) {
      println("WARNING: unable to prepare for compound ranking: series size mismatch: " +
        s"$s1 and $s2")
      (Seq.empty, Seq.empty)
    } else {
      d1.zip(d2).filter(x => x._1.present && x._2.present).unzip
    }
  }
}
