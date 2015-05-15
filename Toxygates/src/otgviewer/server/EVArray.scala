package otgviewer.server

import t.common.shared.sample.ExpressionValue

object EVArray {
  def apply(evs: Seq[ExpressionValue]): EVArray = {
    val eva = evs.toArray
    new EVArray(eva.map(_.getValue), eva.map(_.getCall),
        eva.map(_.getTooltip))        
  }
}

//TODO For scalability, think about avoiding the tooltips 
//since they are not shared, and not primitives
class EVArray(values: Array[Double],
  calls: Array[Char],
  tooltips: Array[String]) extends Seq[ExpressionValue] {
  
  def apply(i: Int): ExpressionValue = 
    new ExpressionValue(values(i), calls(i), tooltips(i))
  
  override def take(n: Int): EVArray = 
    new EVArray(values.take(n), calls.take(n), tooltips.take(n))
  
  override def drop(n: Int): EVArray = 
    new EVArray(values.drop(n), calls.drop(n), tooltips.drop(n))
  
  def length: Int = values.length
  
  def iterator: Iterator[ExpressionValue] = (0 until length).map(apply).iterator
}