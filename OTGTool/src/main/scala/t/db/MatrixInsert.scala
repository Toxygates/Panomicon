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

import t.AtomicTask
import t.Task

/*
 * Note: this is currently only used in tests.
 */
abstract class BasicValueInsert[E <: ExprValue](val db: MatrixDBWriter[E],
    raw: ColumnExpressionData)(implicit mc: MatrixContext) extends MatrixInsert[E](raw)

/**
 * Insert values consisting of an intensity, a p-value, and a call, i.e. PExprValue.
 * All expression database files currently use this format.
 */
class SimpleValueInsert(getDB: () => MatrixDBWriter[PExprValue], raw: ColumnExpressionData)
(implicit mc: MatrixContext) extends MatrixInsert[PExprValue](raw) {
  def mkValue(v: FoldPExpr) =
    PExprValue(v._1, v._3, v._2)

   //Importantly, this does not get initialised until  MatrixInsert.insert actually runs
    //(which means the releasing finalizer will also run)
  lazy val db = getDB()
}

abstract class MatrixInsert[E <: ExprValue](raw: ColumnExpressionData)
(implicit context: MatrixContext) {

  def db: MatrixDBWriter[E]

  protected def mkValue(v: FoldPExpr): E

  private val pmap = context.probeMap
  def packProbe(knownProbes: Set[String], probe: ProbeId,
                log: String => Unit): Option[Int] = {
    if (knownProbes.contains(probe)) {
      val packed = try {
        pmap.pack(probe)
      } catch {
        case lf: LookupFailedException =>
          throw new LookupFailedException(
            s"Unknown probe: $probe. Did you forget to upload a platform definition?")
        case e: Exception => throw e
      }
      Some(packed)
    } else {
      log(s"Not inserting unknown probe '$probe'")
      None
    }
  }

  def insert(name: String): Task[Unit] = {
    new AtomicTask[Unit](name) {
      override def run(): Unit = {
        try {
          val nsamples = raw.samples.size
          log(s"$nsamples samples")
          log(raw.probes.size + " probes")

          val unknownProbes = raw.probes.toSet -- context.probeMap.tokens
          for (probe <- (unknownProbes take 100)) {
            log(s"Warning: unknown probe '$probe' (This error may be safely ignored. The probe will be skipped.)")
            log(s"Total of ${unknownProbes.size} unknown probes.")
          }
          val knownProbes = raw.probes.toSet -- unknownProbes
          if (knownProbes.isEmpty) {
            throw new LookupFailedException("No valid probes in data. Unable to insert any data.")
          }

          var pcomp = 0d
          var nvalues = 0

          for {
            (sample, data) <- raw.samplesAndData
            if shouldContinue(pcomp)
            withProbes = data.map { case (p, v) => (packProbe(knownProbes, p, x => log(x)), v) }
            values = withProbes.collect {case (Some(probe), (v, c, p)) => (probe, mkValue(v, c, p)) }
          } {
            nvalues += values.size
            db.writeMany(sample, values)
            pcomp += 100.0 / nsamples
          }

          logResult(s"${nvalues} values written")
        } finally {
          db.release()
        }
      }
    }
  }
}
