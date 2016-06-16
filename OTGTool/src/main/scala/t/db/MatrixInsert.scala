/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import t.Tasklet
import t.db.kyotocabinet.KCMatrixDB

object MatrixInsert {
  @deprecated("Use DataConfig methods", "13 April 2016")
  def matrixDB(fold: Boolean, dbfile: String)(implicit mc: MatrixContext): MatrixDBWriter[_] =
    if (fold) {
      KCMatrixDB.getExt(dbfile, true)
    } else {
      KCMatrixDB.get(dbfile, true)
    }
}

//TODO possibly deprecate this. Currently used only in tests
abstract class BasicValueInsert[E <: ExprValue](val db: MatrixDBWriter[E],
    raw: RawExpressionData)(implicit mc: MatrixContext) extends MatrixInsert[E](raw)

//TODO possibly deprecate this. Currently used only in tests
class AbsoluteValueInsert(dbfile: String, raw: RawExpressionData)
(implicit mc: MatrixContext) extends MatrixInsert[BasicExprValue](raw) {
  lazy val db = KCMatrixDB.get(dbfile, true)

  def mkValue(v: FoldPExpr) =
    BasicExprValue(v._1, v._2)
}

/**
 * As above but with p-values.
 */
class SimplePFoldValueInsert(getDB: () => MatrixDBWriter[PExprValue], raw: RawExpressionData)
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

  def values(x: Sample) =
    for (
      (probe, (v, c, p)) <- raw.data(x)
    ) yield (probe, mkValue(v, c, p))

  def insert(name: String): Tasklet = {
    new Tasklet(name) {
      def run() {
        try {
          val data = raw.data
          val ns = data.keySet.size
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
          val it = raw.data.keys.iterator
          while (it.hasNext && shouldContinue(pcomp)) {
            val sample = it.next
            val vs = values(sample)

            val packed = vs.flatMap(vv => {
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
                Some(sample, pk, v)
              } else {
                log(s"Not inserting unknown probe '$probe'")
                None
              }
            })
            nvalues += packed.size
            db.writeMany(packed)
            pcomp += 100.0 / ns
          }

          logResult(s"${nvalues} values written")
        } finally {
          db.release()
        }
      }
    }
  }
}
