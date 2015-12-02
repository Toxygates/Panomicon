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
  def matrixDB(fold: Boolean, dbfile: String)(implicit mc: MatrixContext): MatrixDBWriter[_] =
    if (fold) {
      KCMatrixDB.getExt(dbfile, true)
    } else {
      KCMatrixDB.get(dbfile, true)
    }
}

abstract class BasicValueInsert[E <: ExprValue](val db: MatrixDBWriter[E],
    raw: RawExpressionData)(implicit mc: MatrixContext) extends MatrixInsert[E](raw)

class AbsoluteValueInsert(dbfile: String, raw: RawExpressionData)
(implicit mc: MatrixContext) extends MatrixInsert[BasicExprValue](raw) {
  lazy val db = KCMatrixDB.get(dbfile, true)

  def mkValue(v: Double, c: Char, p: Double) =
    BasicExprValue(v, c)
}

/**
 * As above but with p-values.
 */
class SimplePFoldValueInsert(getDB: () => MatrixDBWriter[PExprValue], raw: RawExpressionData)
(implicit mc: MatrixContext) extends MatrixInsert[PExprValue](raw) {
  def mkValue(v: Double, c: Char, p: Double) =
    PExprValue(v, p, c)

   //Importantly, this does not get initialised until  MatrixInsert.insert actually runs
    //(which means the releasing finalizer will also run)
    //The same is true for AbsoluteValueInsert above
  lazy val db = getDB()
}

abstract class MatrixInsert[E <: ExprValue](raw: RawExpressionData)
(implicit context: MatrixContext) {

  def db: MatrixDBWriter[E]

  protected def mkValue(v: Double, c: Char, p: Double): E

  private lazy val values =
    for (
      x <- raw.data.keys;
      (probe, (v, c, p)) <- raw.data(x)
    ) yield (x, probe, mkValue(v, c, p))

  def insert(name: String): Tasklet = {
    new Tasklet(name) {
      def run() {
        try {
          val data = raw.data
          log(data.keySet.size + " samples")
          log(raw.probes.size + " probes")

          val total = values.size
          val pmap = context.probeMap
          var pcomp = 0d
          val it = values.iterator
          while (it.hasNext && shouldContinue(pcomp)) {
            val (x, probe, v) = it.next
            val packed = try {
              pmap.pack(probe)
            } catch {
              case lf: LookupFailedException =>
                throw new LookupFailedException(
                  s"Unknown probe: $probe. Did you forget to upload a platform definition?")
              case t: Throwable => throw t
            }

            db.write(x, packed, v)
            pcomp += 100.0 / total
          }

          logResult(s"${values.size} values")
        } finally {
          db.release()
        }
      }
    }
  }
}
