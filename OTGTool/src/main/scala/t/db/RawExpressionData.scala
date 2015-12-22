/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.db

import scala.collection.{ Map => CMap }

/**
 * RawExpressionData is sample data that has not yet been inserted into the database.
 */
trait RawExpressionData {

  lazy val asExtValues: CMap[Sample, CMap[String, PExprValue]] =
    data.mapValues(_.map(p => p._1 -> PExprValue(p._2._1, p._2._3, p._2._2, p._1)))

  /**
   * Map samples to (probe -> (expr value, call, p))
   */
  def data: CMap[Sample, CMap[String, (Double, Char, Double)]]

  def call(x: Sample, probe: String) = data(x)(probe)._2
  def expr(x: Sample, probe: String) = data(x)(probe)._1
  def p(x: Sample, probe: String) = data(x)(probe)._3

  lazy val probes: Iterable[String] =
    data.toSeq.flatMap(_._2.keys).distinct

  def samples: Iterable[Sample] = data.keys

}

class Log2Data(raw: RawExpressionData) extends RawExpressionData {
  private val log2 = Math.log(2)
  private def l2(x: Double) = Math.log(x) / log2

  def data = raw.data.mapValues(_.mapValues(x => (l2(x._1), x._2, x._3)))
}
