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
   * 
   * TODO should this be FoldPExpr or a simpler type e.g. ExprValue?
   * No RawExpression data implementation supports reading p-values.
   */
  def dataMap: CMap[Sample, CMap[String, FoldPExpr]] =
    Map() ++ samples.map(s => s -> data(s))

  def data(s: Sample): CMap[String, FoldPExpr]

  def data(ss: Iterable[Sample]): CMap[Sample, CMap[String, FoldPExpr]] =
    Map() ++ ss.map(s => s -> data(s))

  /**
   * Obtain a call value.
   */
  def call(x: Sample, probe: String): Option[Char] = data(x).get(probe).map(_._2)

  /**
   * Obtain an expression value.
   */
  def expr(x: Sample, probe: String): Option[Double] = data(x).get(probe).map(_._1)

  def probes: Iterable[String] = probesInSamples

  lazy val probesInSamples =
    samples.toSeq.flatMap(data(_).keys).distinct

  def samples: Iterable[Sample]
}

/**
 * Adds methods for efficient sample-based lookup.
 * Values are returned in the order specified by the probes sequence.
 */
trait ColumnExpressionData {
  def probes: Seq[String] 
  def samples: Iterable[Sample]
  
  /**
   * Pre-cache data
   */
  def loadData(ss: Iterable[Sample]) {}
  
  def data(s: Sample): CMap[String, FoldPExpr]
  
  def data(ss: Iterable[Sample]): CMap[Sample, CMap[String, FoldPExpr]]
  
  /**
   * Obtain calls for all probes.
   */
  def calls(x: Sample): Seq[Option[Char]] 
  
  /**
   * Obtain expression values for all probes.
   */
  def exprs(x: Sample): Seq[Option[Double]] 
 
  /**
   * Release the resource after use.
   */
  def release() {}
}

class Log2Data(raw: RawExpressionData) extends RawExpressionData {
  override def data(s: Sample) = raw.data(s).mapValues(x => (ExprValue.log2(x._1), x._2, x._3))

  def samples = raw.samples
}
