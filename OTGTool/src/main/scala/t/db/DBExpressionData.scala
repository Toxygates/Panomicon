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

import t.db._
import scala.collection.mutable.{Set => MSet}

class DBExpressionData(reader: MatrixDBReader[ExprValue], val requestedSamples: Iterable[Sample],
    val requestedProbes: Iterable[Int]) {

  val samples: Iterable[Sample] = reader.sortSamples(requestedSamples)

  private lazy val exprValues: Iterable[Iterable[ExprValue]] = {
    val np = requestedProbes.size
    val ns = requestedSamples.size
    logEvent(s"Requesting $np probes for $ns samples")
    
    val r = reader.valuesInSamples(samples, reader.sortProbes(requestedProbes), false)
    
    val present = r.map(_.count(!_.isPadding)).sum
    
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

  lazy val probes: Iterable[String] = {
    var r = MSet[String]()
    for ((sample, lookup) <- _data) {
      r ++= lookup.keys
    }
    r.toSeq
  }

  def data(s: Sample): Map[String, FoldPExpr] = _data(s)
    
  def logEvent(message: String) {}
  
  def close() { reader.release() }
}

import scala.collection.{Map => CMap}

/**
 * Preferred choice for sample-major access.
 * Inefficient for other use cases.
 *
 */
class DBColumnExpressionData(reader: MatrixDBReader[ExprValue],
  requestedSamples: Iterable[Sample],
   requestedProbes: Iterable[Int]) extends ColumnExpressionData {
  
  override val samples: Iterable[Sample] = reader.sortSamples(requestedSamples)
  
  val codedProbes = reader.sortProbes(requestedProbes)
  override val probes = 
     codedProbes.map(reader.probeMap.unpack)
  
  var currentSamples: Seq[Sample] = Seq()
  var currentCalls: Seq[Seq[Option[Char]]] = Seq()
  var currentExprs: Seq[Seq[Option[Double]]] = Seq()
  
  def logEvent(s: String) { println(s) }
  
  /**
   * Pre-cache data for the given samples.
   * Should be a subset of the samples requested at construction.
   * Does nothing if the requested samples have already been cached.
   */
  override def loadData(ss: Iterable[Sample]) {
    if (!((ss.toSet -- currentSamples.toSet).isEmpty)) {
      val loadSamples = reader.sortSamples(ss)
      logEvent(s"DB read $loadSamples")
      val d = reader.valuesInSamples(loadSamples, codedProbes, true)
      currentSamples = loadSamples
      currentCalls = d.map(_.map(_.paddingOption.map(_.call)).toSeq).toSeq
      currentExprs = d.map(_.map(_.paddingOption.map(_.value)).toSeq).toSeq
    }
  }
  
  def data(s: Sample): CMap[String, FoldPExpr] = {
    loadData(Seq(s))
    val pec = (probes zip (exprs(s) zip calls(s)))
    Map() ++ pec.collect { case (p, (Some(exp), Some(call))) => 
      p -> (exp, call, 0.0)
    }
  }
  
  /**
   * Obtain calls for all probes.
   */
  override def calls(x: Sample): Seq[Option[Char]] = {
    loadData(Seq(x))
    currentCalls(currentSamples indexOf x)
  }
  
  /**
   * Obtain expression values for all probes.
   */
  override def exprs(x: Sample): Seq[Option[Double]] = {
    loadData(Seq(x))
    currentExprs(currentSamples indexOf x)
  }
  
  override def release() { reader.release() }
}