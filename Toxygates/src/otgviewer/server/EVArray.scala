package otgviewer.server

import t.common.shared.sample.ExpressionValue
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.collection.mutable.ArrayBuilder

object EVABuilder extends CanBuildFrom[Seq[ExpressionValue], ExpressionValue, EVArray] {
  def apply() = new EVABuilder 
  
  def apply(s: Seq[ExpressionValue]): EVABuilder = 
    new EVABuilder      
}

class EVABuilder(_values: Seq[Double] = Seq(),
    _calls: Seq[Char] = Seq(),
    _tooltips: Seq[String] = Seq()) extends Builder[ExpressionValue, EVArray] {
  
  private var values = new ArrayBuilder.ofDouble ++= _values
  private var calls = new ArrayBuilder.ofChar ++= _calls
  private var tooltips = new ArrayBuilder.ofRef[String] ++= _tooltips
  
  def clear(): Unit = {
    values.clear()
    calls.clear()
    tooltips.clear()
  }
  
  def += (x: ExpressionValue): this.type = {
    values += x.getValue
    calls += x.getCall
    tooltips += x.getTooltip
    this
  }
  
  override def sizeHint(h: Int): Unit = {
    values.sizeHint(h)
    calls.sizeHint(h)
    tooltips.sizeHint(h)
  }
  
  def result = new EVArray(values.result, calls.result, tooltips.result)  
}

object EVArray {
  def apply(evs: Seq[ExpressionValue]): EVArray = 
    (new EVABuilder ++= evs).result      
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