package t.db.testing

import t.db.MatrixDB
import t.db.ExprValue
import otg.Species._
import otg.Context
import t.db.BasicExprValue
import t.db.ProbeMap
import t.db.Sample

abstract class AbsFakeMatrixDB[E >: Null <: ExprValue](implicit val probeMap: ProbeMap) extends MatrixDB[E, E] {
  var closed = false
  var released = false
  var records: Vector[(Sample, Int, E)] = Vector()
  
  def emptyValue(probe: String): E 
 
  def sortSamples(xs: Iterable[Sample]): Seq[Sample] = xs.toSeq
  
  def allSamples: Iterable[Sample] = records.map(_._1).toSet
    
  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, E)] = {
    null
  }
  
  def valuesInSample(x: Sample, probes: Iterable[Int]): Iterable[E] =
    records.filter(_._1 == x).map(x => x._3)
  
  def write(s: Sample, probe: Int, e: E) {
    records :+= (s, probe, e)
  }
  
  def close() {
    closed = true
    println("Fake DB closed")
  }
  
  def release() {
    released = true
    println("Fake DB released")
  }
}

class FakeBasicMatrixDB(implicit probes: ProbeMap) extends AbsFakeMatrixDB[BasicExprValue] {
  def emptyValue(probe: String) = ExprValue(Double.NaN, 'A', probe)
  
  def deleteSample(x: Sample): Unit = {}
}