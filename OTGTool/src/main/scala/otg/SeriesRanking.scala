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
import t.db.SeriesDB
import t.db.Series
import t.db.ExprValue
import t.db.MatrixContext
import t.util.SafeMath

class SeriesRanking(override val db: SeriesDB[OTGSeries], override val key: OTGSeries)
(implicit context: OTGContext) extends t.SeriesRanking[OTGSeries](db, key) {
  import Statistics._
  import SafeMath._
  import t.SeriesRanking._

  def packProbe(p: String): Int = context.probeMap.pack(p)
  def withProbe(newProbe: Int) = new SeriesRanking(db, key.copy(probe = newProbe))
  def withProbe(newProbe: String) = new SeriesRanking(db, key.copy(probe = packProbe(newProbe)))

  override protected def getScores(mt: RankType): Iterable[(OTGSeries, Double)] = {
    mt match {
      case r: ReferenceCompound[OTGSeries] => {
        r.init(db, key.copy(compound = r.compound, dose = r.dose)) //init this once and reuse it across all the compounds
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
    val doses = allScores.flatMap(_.map(_._1.dose)).distinct
    val compounds = allScores.flatMap(_.map(_._1.compound)).distinct
    val dcs = for (d <- doses; c <- compounds) yield (d, c)
    val products = dcs.map(dc => {
      //TODO efficiency of this
      val allCorresponding = allScores.map(_.find(x =>
        x._1.dose == dc._1 && x._1.compound == dc._2))
      val product = safeProduct(allCorresponding.map(_.map(_._2).getOrElse(Double.NaN)))
      (dc._2, dc._1, product)
    })
    val byCompound = products.groupBy(_._1)
    byCompound.map(x => {
      val sort = x._2.toList.sortWith(_._3 > _._3)
      //highest scoring dose for each compound
      sort.head
    })
  }
}
