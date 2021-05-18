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

package t

import t.util.SafeMath
import t.db._
import friedrich.data.Statistics

class SeriesRanking(val db: SeriesDB[OTGSeries], val key: OTGSeries)
                   (implicit context: MatrixContext)  {
  import SafeMath._
  import t.SeriesRanking._

  def packProbe(p: String): Int = context.probeMap.pack(p)
  def withProbe(newProbe: Int) = new SeriesRanking(db, key.copy(probe = newProbe))
  def withProbe(newProbe: String) = new SeriesRanking(db, key.copy(probe = packProbe(newProbe)))

  def loadRefCurves(key: OTGSeries) = {
    val refCurves = db.read(key)
    println("Initialised " + refCurves.size + " reference curves")
    refCurves.toList
  }

  protected def getScores(mt: RankType): Iterable[(OTGSeries, Double)] = {
    mt match {
      case r: ReferenceCompound => {
        //Init this once and reuse across all the compounds
        r.refCurves = loadRefCurves(key.copy(compound = r.compound,
          doseOrTime = r.doseOrTime))
      }
      case _ => {}
    }
    mt.matchOneProbe(this, key.probe)
  }

  /**
   * This method is currently the only entry point used by the web application.
   * Returns (compound, dose, score)
   */
  def rankCompoundsCombined(probesRules: Seq[(String, RankType)]): Iterable[(String, String, Double)] = {

    // Get scores for each rule
    val allScores = probesRules.map(pr => withProbe(pr._1).getScores(pr._2))
    val dosesOrTimes = allScores.flatMap(_.map(_._1.doseOrTime)).distinct
    val compounds = allScores.flatMap(_.map(_._1.compound)).distinct

    //We score each combination of compounds and fixed doses (for time series)
    //or fixed times (for dose series) independently

    val products = (for (
      dt <- dosesOrTimes; c <- compounds;
      allCorresponding = allScores.map(_.find(series =>
        series._1.doseOrTime == dt && series._1.compound == c));
      scores = allCorresponding.map(_.map(_._2).getOrElse(Double.NaN));
      product = safeProduct(scores.toList);
      result = (c, dt, product)
    ) yield result)

    val byCompound = products.groupBy(_._1)
    byCompound.map(x => {
      //highest scoring dose or time for each compound
      //NaN values must be handled properly
      x._2.sortWith((a, b) => safeIsGreater(a._3, b._3)).head
    })
  }
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
    def matchOneProbe(c: SeriesRanking, probe: Int): Iterable[(OTGSeries, Double)] = {
      val key = c.key.asSingleProbeKey
      val data = c.db.read(key)
      println(s"Read for key: $key result size ${data.size}")

      scoreAllSeries(data.toSeq)
    }

    def scoreAllSeries(series: Seq[OTGSeries]): Iterable[(OTGSeries, Double)] = {
      series.map(s => (s, scoreSeries(s)))
    }

    def scoreSeries(s: OTGSeries): Double
  }

  case class MultiSynthetic(pattern: Vector[Double]) extends RankType {
    def scoreSeries(s: OTGSeries): Double =
      safePCorrelation(pattern.toList, s.values.map(_.value)) + 1
  }

  object Sum extends RankType {
    // Note: numbers like 50 are ad hoc to 'guarantee' a positive result
    def scoreSeries(s: OTGSeries): Double =
      (50 + safeMean(s.presentValues)) / 50
  }

  object NegativeSum extends RankType {
    def scoreSeries(s: OTGSeries): Double =
      (50 - safeMean(s.presentValues)) / 50
  }

  object Unchanged extends RankType {
    def scoreSeries(s: OTGSeries): Double =
      (1000 - safeSum(s.presentValues.map(x => x * x))) / 1000
  }

  /**
   * This match type maximises standard deviation.
   */
  object HighVariance extends RankType {
    def scoreSeries(s: OTGSeries): Double =
      (10 + safeSigma(s.presentValues)) / 10
  }

  /**
   * This match type minimises standard deviation.
   */
  object LowVariance extends RankType {
    def scoreSeries(s: OTGSeries): Double =
      (10 - safeSigma(s.presentValues)) / 10
  }

  object MonotonicIncreasing extends RankType {
    def scoreSeries(s: OTGSeries): Double = {
      var score = 5
      if (s.values(0).value < 0 || s.values(0).call == 'A') { //optional: remove this constraint
        score -= 1
      }

      for (Seq(fst, snd) <- s.values.sliding(2)) {
        if (snd.value < fst.value - 0.001 ||
            snd.value < 0.001 ||
            snd.call == 'A') {
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
    def scoreSeries(s: OTGSeries): Double = {
      var score = 5
      if (s.values(0).value > 0 || s.values(0).call == 'A') { //optional: remove this constraint
        score -= 1
      }

      for (Seq(fst, snd) <- s.values.sliding(2)) {
        if (snd.value > fst.value + 0.001 ||
            snd.value > 0.001 ||
            snd.call == 'A') {
          score -= 1
        }
      }

      //score -= (4 - s.values.size) //penalise missing data heavily
      score
    }
  }

  object MinFold extends RankType {
    def scoreSeries(s: OTGSeries): Double = 20 - safeMin(s.presentValues)
  }

  object MaxFold extends RankType {
    def scoreSeries(s: OTGSeries): Double = 20 + safeMax(s.presentValues)
  }

  class ReferenceCompound(val compound: String, val doseOrTime: String,
      var refCurves: List[Series[_]] = List()) extends RankType {

    def scoreSeries(s: OTGSeries): Double = {
      if (refCurves.isEmpty) {
        Console.err.println("Warning: no reference curves available")
      }
      safeMax(refCurves.map(rc => safePCorrelation(rc, s) + 1))
    }
  }

  def safePCorrelation(s1: List[Double], s2: List[Double]): Double = {
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
    safePCorrelation(mut1.map(_.value), mut2.map(_.value))
  }

  def mutualPoints(s1: Series[_], s2: Series[_]): (List[ExprValue], List[ExprValue]) = {
    val (d1, d2) = (s1.values.toList, s2.values.toList)
    // pick the matching points to perform ranking
    if (d1.size != d2.size) {
      println("WARNING: unable to prepare for compound ranking: series size mismatch: " +
        s"$s1 and $s2")
      (Nil, Nil)
    } else {
      d1.zip(d2).filter(x => x._1.present && x._2.present).unzip
    }
  }
}
