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

import scala.collection.{ Map => CMap }

/**
 * RawExpressionData is sample data that has not yet been inserted into the database.
 */
trait RawExpressionData {

  lazy val asExtValues: CMap[Sample, CMap[String, PExprValue]] =
    dataMap.mapValues(_.map(p => p._1 -> PExprValue(p._2._1, p._2._3, p._2._2, p._1)))

  /**
   * Map samples to (probe -> (expr value, call, p))
   */
  def dataMap: CMap[Sample, CMap[String, FoldPExpr]] =
    Map() ++ samples.map(s => s -> data(s))

  def data(s: Sample): CMap[String, FoldPExpr]

  def data(ss: Iterable[Sample]): CMap[Sample, CMap[String, FoldPExpr]] =
    Map() ++ ss.map(s => s -> data(s))

  /**
   * Obtain a call value. Need not necessarily be present.
   */
  def call(x: Sample, probe: String) = data(x).get(probe).map(_._2)
  
  /**
   * Obtain an expression value. Need not necessarily be present.
   */
  def expr(x: Sample, probe: String): Option[Double] = data(x).get(probe).map(_._1)
  
  /**
   * Obtain a p-value. Need not necessarily be present.
   */
  def p(x: Sample, probe: String) = data(x).get(probe).map(_._3)

  def probes: Iterable[String] = probesInSamples

  lazy val probesInSamples =
    samples.map(data(_)).toSeq.flatMap(_.keys).distinct

  def samples: Iterable[Sample]

  /**
   * An in-memory snapshot of this data.
   */
  def cached: CachedRawData = new CachedRawData(dataMap)

  /**
   * An in-memory snapshot of this data for a set of samples.
   */
  def cached(samples: Iterable[Sample]): CachedRawData = {
    new CachedRawData(data(samples))
  }
}

class Log2Data(raw: RawExpressionData) extends RawExpressionData {
  private val log2 = Math.log(2)
  private def l2(x: Double) = Math.log(x) / log2

  override def data(s: Sample) = raw.data(s).mapValues(x => (l2(x._1), x._2, x._3))

  def samples = raw.samples
}

class CachedRawData(raw: CMap[Sample, CMap[String, FoldPExpr]]) extends RawExpressionData {
  override def dataMap = raw
  override def data(s: Sample) = raw(s)
  override def samples = raw.keys
}
