package otgviewer.server

import otg.db.MicroarrayDBReader
import otg.db.ExtMicroarrayDBReader
import otg.PExprValue
import otg.ExprValue
import bioweb.shared.array.ExpressionValue
import otg.Sample
import otg.Species

object ExpressionValueReader {
  /**
   * Construct an ExpressionValueReader that fits the given DB type.
   */
  def apply[E <: ExprValue](reader: MicroarrayDBReader[E]): ExpressionValueReader[_ <: ExprValue] = 
    reader match {
    case emd: ExtMicroarrayDBReader => new Converter[PExprValue](emd) {
      def convert(v: PExprValue) = new ExpressionValue(v.value, v.call, v.p)
    }
    case _ => new Converter[ExprValue](reader) {
      def convert(v: ExprValue) = new ExpressionValue(v.value, v.call)
    }
  }
}

/**
 * An ExpressionValueReader wraps a MicroarrayDBReader so that the latter can be 
 * queried for ExpressionValues.
 */
trait ExpressionValueReader[E <: ExprValue] {
  def db: MicroarrayDBReader[E]
  
  def close() { db.close() }
  def presentValuesForSamplesAndProbes(s: Species, xs: Seq[Sample], 
      probes: Seq[Short], sparseRead: Boolean = false): Vector[Vector[ExpressionValue]] = {
		db.presentValuesForSamplesAndProbes(s, xs, probes, sparseRead).map(_.map(convert))
  }
  
  protected def convert(v: E): ExpressionValue
}

abstract class Converter[E <: ExprValue](val db: MicroarrayDBReader[E]) 
extends ExpressionValueReader[E] {
  protected def convert(v: E): ExpressionValue    
}
