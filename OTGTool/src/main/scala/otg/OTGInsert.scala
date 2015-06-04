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

package otg

import scala.annotation.migration

import t.db.RawExpressionData
import t.Tasklet
import t.db.BasicExprValue
import t.db.ExprValue
import t.db.LookupFailedException
import t.db.MatrixContext
import t.db.MatrixDBWriter
import t.db.PExprValue
import t.db.Sample
import t.db.kyotocabinet.KCMatrixDB

/**
 * TODO A lot of code here should be ported over to the T section
 */
object OTGInsert {

  class InsertionContext[E <: ExprValue](val db: () => MatrixDBWriter[E],
      val builder: ExprValueBuilder[E], val inserter: MicroarrayInsertion[E]) {
    
    def insert(raw: RawExpressionData): Tasklet = 
       inserter.insertFrom("Insert normalized intensity", raw, builder)
    
    def insertFolds(raw: RawExpressionData): Tasklet = 
       inserter.insertFrom("Insert folds", raw, builder)
  }

  //TODO this method and the next one are messy, clean up
  def matrixDB(fold: Boolean, dbfile: String)
  (implicit mc: MatrixContext): MatrixDBWriter[_] = 
    KCMatrixDB(dbfile, true, fold)      
  
  def insertionContext(fold: Boolean, dbfile: String)
  (implicit mc: MatrixContext): InsertionContext[_] = {	
    if (fold) {      
      val db = () => KCMatrixDB.applyExt(dbfile, true) 
      new InsertionContext(
        db, new SimplePValueBuilder(), new MicroarrayInsertion(db))
    } else {      
      val db = () => KCMatrixDB(dbfile, true)
      new InsertionContext(
        db, new AbsoluteValueBuilder(), new MicroarrayInsertion(db))
    }
  }
}

trait ExprValueBuilder[E <: ExprValue] {
  /**
   * Construct the values to insert for a group of related control and treated samples
   * (for example: acetaminophen, 24h, high dose, rat, in vivo, liver, single)
   */
  def values(data: RawExpressionData): Iterable[(Sample, String, E)]
}

/**
 * Build straightforward ExprValues with no p-values.
 */
class AbsoluteValueBuilder extends ExprValueBuilder[BasicExprValue] {
  def values(data: RawExpressionData) = {     
    for (x <- data.data.keys;
      (probe, (v, c, p)) <- data.data(x)) yield (x, probe, BasicExprValue(v, c))      
  }
}

/**
 * As above but with p-values.
 */
class SimplePValueBuilder extends ExprValueBuilder[PExprValue] {
  def values(data: RawExpressionData) = {
     for (x <- data.data.keys;
      (probe, (v, c, p)) <- data.data(x)) yield (x, probe, PExprValue(v, p, c))      
  }
}

class MicroarrayInsertion[E <: ExprValue](dbGetter: () => MatrixDBWriter[E])
	(implicit context: MatrixContext) {
  
  def insertFrom(name: String,  
      raw: RawExpressionData, builder: ExprValueBuilder[E]) =
    new Tasklet(name) {
      def run() {        
        val db = dbGetter()
        try {
          val vals = builder.values(raw)
          val data = raw.data
          log(data.keySet.size + " samples")
          log(raw.probes.size + " probes")

          val total = vals.size
          val pmap = context.probeMap
          var pcomp = 0d
          val it = vals.iterator
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

          logResult(s"${vals.size} values")
        } finally {
          db.release()
        }
      }    
  }
}