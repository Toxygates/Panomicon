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
 * Efficient sample-based lookup from a data source.
 * For some methods, values are returned in the order specified by the probes sequence.
 */
trait ColumnExpressionData {
  def probes: Seq[String] 
  def samples: Iterable[Sample]
  
  /**
   * Pre-cache data for efficiency, if the implementation supports it.
   * Calling this method is optional.
   */
  def loadData(ss: Iterable[Sample]) {}
  
  def data(s: Sample): CMap[String, FoldPExpr]
  
  def data(ss: Iterable[Sample]): CMap[Sample, CMap[String, FoldPExpr]] = {
    loadData(ss)
    Map() ++ ss.map(s => s -> data(s))
  }
 
  /**
   * Obtain calls for all probes.
   * Default implementation for convenience, may be overridden
   */
  def calls(x: Sample): Seq[Option[Char]] = {
    val d = data(x)
    probes.map(p => d.get(p).map(_._2))
  }
  
  /**
   * Obtain expression values for all probes.
   * Default implementation for convenience, may be overridden
   */
  def exprs(x: Sample): Seq[Option[Double]] = {
    val d = data(x)
    probes.map(p => d.get(p).map(_._1))
  }
 
  /**
   * Release the resource after use.
   */
  def release() {}
  
  /**
   * Used mainly by tests
   */
  def asExtValue(s: Sample, probe: String) = { 
    val v = data(s).get(probe)
    v.map(v => PExprValue(v._1, v._3, v._2, probe))
  }
  
  /**
   * Used mainly by tests
   */
  def asExtValues(s: Sample): CMap[String, PExprValue] =
    Map() ++ data(s).toSeq.map(p => p._1 -> PExprValue(p._2._1, p._2._3, p._2._2, p._1))
}

/**
 * This style of log2 computation was used historically in Tritigate
 * but is not currently used in Toxygates.
 */
class Log2Data(raw: ColumnExpressionData) extends ColumnExpressionData {
  def probes = raw.probes
  
  override def data(s: Sample) = raw.data(s).mapValues(x => (ExprValue.log2(x._1), x._2, x._3))

  def samples = raw.samples
}
