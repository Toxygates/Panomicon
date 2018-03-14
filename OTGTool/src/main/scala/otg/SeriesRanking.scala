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

import friedrich.data.Statistics
import t.util.SafeMath
import t.db._

class SeriesRanking(override val db: SeriesDB[OTGSeries], override val key: OTGSeries)
(implicit context: OTGMatrixContext) extends t.SeriesRanking[OTGSeries](db, key) {
  import Statistics._
  import SafeMath._
  import t.SeriesRanking._

  def packProbe(p: String): Int = context.probeMap.pack(p)
  def withProbe(newProbe: Int) = new SeriesRanking(db, key.copy(probe = newProbe))
  def withProbe(newProbe: String) = new SeriesRanking(db, key.copy(probe = packProbe(newProbe)))

  override protected def getScores(mt: RankType): Iterable[(OTGSeries, Double)] = {
    mt match {
      //TODO modify ReferenceCompound API to be able to check for type argument at runtime
      //or stop trying to match the type argument
      case r: ReferenceCompound[OTGSeries @unchecked] => {
        r.init(db, key.copy(compound = r.compound, doseOrTime = r.doseOrTime)) //init this once and reuse it across all the compounds
      }
      case _ => {}
    }
    super.getScores(mt)
  }

  /**
   * This method is currently the only entry point used by the web application.
   * Returns (compound, dose, score)
   */
  override def rankCompoundsCombined(probesRules: Seq[(String, RankType)]): Iterable[(String, String, Double)] = {

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
      product = safeProduct(scores);
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
