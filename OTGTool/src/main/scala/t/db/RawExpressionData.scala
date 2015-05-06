package t.db

import scala.collection.{Map => CMap}

/**
 * RawExpressionData is sample data that has not yet been inserted into the database.
 * 
 * TODO: Move to t.db
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