/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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
    raw: RawExpressionData)(implicit mc: MatrixContext) extends MatrixInsert[E](raw)

/**
 * Insert values consisting of an intensity, a p-value, and a call, i.e. PExprValue.
 * All expression database files currently use this format.
 */
class SimpleValueInsert(getDB: () => MatrixDBWriter[PExprValue], raw: RawExpressionData)
(implicit mc: MatrixContext) extends MatrixInsert[PExprValue](raw) {
  def mkValue(v: FoldPExpr) =
    PExprValue(v._1, v._3, v._2)

   //Importantly, this does not get initialised until  MatrixInsert.insert actually runs
    //(which means the releasing finalizer will also run)
    //The same is true for AbsoluteValueInsert above
  lazy val db = getDB()
}

abstract class MatrixInsert[E <: ExprValue](raw: RawExpressionData)
(implicit context: MatrixContext) {

  def db: MatrixDBWriter[E]

  protected def mkValue(v: FoldPExpr): E

  def values(xs: Iterable[Sample]) =
    for ((x, data) <- raw.data(xs);
      values = data.toSeq.map {case (probe, (v, c, p)) => (probe, mkValue(v, c, p)) }
    ) yield (x, values)

  def insert(name: String): Task[Unit] = {
    new AtomicTask[Unit](name) {
      override def run(): Unit = {
        try {
          val ns = raw.samples.size
          log(s"$ns samples")
          log(raw.probes.size + " probes")

          val unknownProbes = raw.probes.toSet -- context.probeMap.tokens
          for (probe <- unknownProbes) {
            log(s"Warning: unknown probe '$probe' (this error may be safely ignored).")
          }
          val knownProbes = raw.probes.toSet -- unknownProbes
          if (knownProbes.size == 0) {
            throw new LookupFailedException("No valid probes in data.")
          }

          val pmap = context.probeMap
          var pcomp = 0d
          var nvalues = 0

          //CSVRawExpressionData is more efficient with chunked reading
          //TODO optimise more for fold building
          val it = raw.samples.iterator.grouped(50)
          while (it.hasNext && shouldContinue(pcomp)) {
            val sampleChunk = it.next
            for ((sample, vs) <- values(sampleChunk);
              if shouldContinue(pcomp)) {
              val packed = vs.toSeq.flatMap(vv => {
                val (probe, v) = vv
                if (knownProbes.contains(probe)) {
                  val pk = try {
                    pmap.pack(probe)
                  } catch {
                    case lf: LookupFailedException =>
                      throw new LookupFailedException(
                        s"Unknown probe: $probe. Did you forget to upload a platform definition?")
                    case t: Throwable => throw t
                  }
                  Some(pk, v)
                } else {
                  log(s"Not inserting unknown probe '$probe'")
                  None
                }
              })
              nvalues += packed.size
              db.writeMany(sample, packed)
              pcomp += 100.0 / ns
            }
          }

          logResult(s"${nvalues} values written")
        } finally {
          db.release()
        }
      }
    }
  }
}
