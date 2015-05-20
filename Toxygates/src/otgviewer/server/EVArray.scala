package otgviewer.server

import t.common.shared.sample.ExpressionValue
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder

object EVABuilder extends CanBuildFrom[Seq[ExpressionValue], ExpressionValue, EVArray] {
  def apply() = new EVABuilder 
  
  def apply(s: Seq[ExpressionValue]): EVABuilder = {
    new EVABuilder(s.toList)
  }
}

class EVABuilder(private var vals: Seq[ExpressionValue] = Seq()) extends Builder[ExpressionValue, EVArray] {
  def clear {
    vals = Seq()
  }
  
  def += (x: ExpressionValue): this.type = {
    vals :+= x
    this
  }
  
  def result = EVArray(vals)  
}

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