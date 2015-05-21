package otgviewer.server

import t.common.shared.sample.ExpressionValue
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder

object EVABuilder extends CanBuildFrom[Seq[ExpressionValue], ExpressionValue, EVArray] {
  def apply() = new EVABuilder 
  
  def apply(s: Seq[ExpressionValue]): EVABuilder = {
    new EVABuilder(s.map(_.getValue), s.map(_.getCall), s.map(_.getTooltip))
  }
}

class EVABuilder(private var values: Seq[Double] = Seq(),
    private var calls: Seq[Char] = Seq(),
    private var tooltips: Seq[String] = Seq()) extends Builder[ExpressionValue, EVArray] {
  
  def clear {
    values = Seq()
    calls = Seq()
    tooltips = Seq()
  }
  
  def += (x: ExpressionValue): this.type = {
    values :+ x.getValue
    calls :+ x.getCall
    tooltips :+ x.getTooltip
    this
  }
  
  def result = new EVArray(values.toArray, calls.toArray, tooltips.toArray)  
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
  
  def length: Int = values.length
  
  def iterator: Iterator[ExpressionValue] = (0 until length).map(apply).iterator
}