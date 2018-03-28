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

package t.db

import t.db._

class DBExpressionData(reader: MatrixDBReader[ExprValue], val requestedSamples: Iterable[Sample],
    val requestedProbes: Iterable[Int])
    extends RawExpressionData {

  val samples: Iterable[Sample] = reader.sortSamples(requestedSamples)

  private lazy val exprValues: Iterable[Iterable[ExprValue]] = {
    val np = requestedProbes.size
    val ns = requestedSamples.size
    logEvent(s"Requesting $np probes for $ns samples")
    
    val r = reader.valuesInSamples(samples, reader.sortProbes(requestedProbes), false)    
    val present = r.map(_.filter(!_.isPadding).size).sum
    
    logEvent(s"Found $present values out of a possible " +
              (np * ns))
    r
  }
  
  private lazy val filteredValues = exprValues.map(_.filter(!_.isPadding))

  /**
   * The number of non-missing expression values found (max = samples * probes)
   */
  lazy val foundValues: Int = filteredValues.map(_.size).sum

  private lazy val _data: Map[Sample, Map[String, FoldPExpr]] = {
    val values: Iterable[Map[String, FoldPExpr]] = filteredValues.map(Map() ++
        _.map(exprValue => exprValue.probe -> (exprValue.value, exprValue.call, 0.0)))

    Map() ++ (samples zip values)
  }

  override lazy val probes: Iterable[String] = _data.values.flatMap(_.keys).toSeq.distinct

  def data(s: Sample): Map[String, FoldPExpr] = _data(s)
  
  def logEvent(message: String) {}
}
