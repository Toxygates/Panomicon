package t.db

import t.sparql.SampleClass

/**
 * A sample.
 */
case class Sample(sampleId: String, sampleClass: SampleClass, cgroup: Option[String]) {

  def dbCode(implicit context: MatrixContext): Int =
    context.sampleMap.pack(sampleId)

  def identifier = sampleId

  override def hashCode: Int = sampleId.hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case s: Sample => sampleId == s.sampleId
      case _ => false
    }
  }

  override def toString = sampleId
}

object Sample {
  def identifierFor(code: Int)(implicit context: MatrixContext): String = {
    context.sampleMap.tryUnpack(code) match {
      case Some(i) => i
      case None => 
        val r = s"unknown_sample[$code]"
        println(r)
        r
    }
  }
  
  def apply(code: Int)(implicit context: MatrixContext): Sample = {
    new Sample(identifierFor(code), SampleClass(), None)
  }
  
  def apply(id: String) = new Sample(id, SampleClass(), None)
  
  def apply(id: String, map: Map[String, String]) = new Sample(id, SampleClass(map), None)
}
