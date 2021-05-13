/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import scala.reflect.ClassTag

/**
 * Wraps a matrix DB to apply a transformation to each value read.
 * Task: remove this mechanism as it makes things more complicated
 */
abstract class TransformingWrapper[E>: Null <: ExprValue : ClassTag](val
    wrapped: MatrixDBReader[E]) extends MatrixDBReader[E] {
  override def allSamples: Iterable[Sample] =
    wrapped.allSamples

  override def emptyValue(probe: ProbeId): E =
    wrapped.emptyValue(probe)

  override def probeMap: ProbeMap =
    wrapped.probeMap

  override def release(): Unit =
    wrapped.release

  override def sortSamples(xs: Iterable[Sample]): Seq[Sample] =
    wrapped.sortSamples(xs)

  def tfmValue(x: E): E

  override  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, E)] = {
    wrapped.valuesForProbe(probe, xs).map(x => (x._1, tfmValue(x._2)))
  }

  override def valuesInSample(x: Sample, probes: Seq[Int],
      padMissingValues: Boolean): Array[E] = {
    wrapped.valuesInSample(x, probes, padMissingValues).map(x => tfmValue(x)).toArray
  }

}
