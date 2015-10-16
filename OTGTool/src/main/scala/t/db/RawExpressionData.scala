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

  /**
   * Map samples to (probe -> (expr value, call, p))
   */
  def data: CMap[Sample, CMap[String, (Double, Char, Double)]]

  def call(x: Sample, probe: String) = data(x)(probe)._2
  def expr(x: Sample, probe: String) = data(x)(probe)._1
  def p(x: Sample, probe: String) = data(x)(probe)._3

  // This assumes that all samples contain the same probe set.
  def probes: Iterable[String] = data.headOption.map(_._2.keys).getOrElse(Iterable.empty)
}
