package t.db

/**
 * Wraps a matrix DB to apply a transformation to each value read.
 */
abstract class TransformingWrapper[E >: Null <: ExprValue](val wrapped: MatrixDBReader[E]) extends MatrixDBReader[E] {
  def allSamples: Iterable[Sample] =
    wrapped.allSamples

  def emptyValue(probe: String): E =
    wrapped.emptyValue(probe)

  def probeMap: ProbeMap =
    wrapped.probeMap

  def release(): Unit =
    wrapped.release

  def sortSamples(xs: Iterable[Sample]): Seq[Sample] =
    wrapped.sortSamples(xs)

  def tfmValue(x: E): E

  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, E)] = {
    wrapped.valuesForProbe(probe, xs).map(x => (x._1, tfmValue(x._2)))
  }

  def valuesInSample(x: Sample, probes: Iterable[Int]): Iterable[E] = {
    wrapped.valuesInSample(x, probes).map(x => tfmValue(x))
  }

}
