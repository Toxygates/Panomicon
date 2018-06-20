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

package t.db.kyotocabinet

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import t.db._
import t.db.testing.TestData

@RunWith(classOf[JUnitRunner])
object KCDBTest extends Matchers {
  import TestData._

  /**
   * General test case for databases that implement MatrixDB.
   */
  def testExtDb(mdb: MatrixDB[PExprValue, PExprValue], d: ColumnExpressionData) {
     for (s <- d.samples; (p, v) <- d.asExtValues(s)) {
      mdb.write(s, probeMap.pack(p), v)
    }

    //test valuesAndProbes
    mdb.allSamples.toSet should equal (samples)
    val sseq = samples.toSeq
    val ppacked = d.probes.map(probeMap.pack).toSeq.sorted

    for (s <- sseq.par; confirm = d.asExtValues(s)) {
      val vs = mdb.valuesForSamplesAndProbes(List(s), ppacked, false, false).flatten
      //In the case of sparse matrices, we may extract additional "absent" values
      //in addition to the ones requested. Hence the subset method is necessary.

      confirm.values.toSet.subsetOf(vs.toSet) should be(true)
    }

    //sparse read
    for (s <- sseq.par; confirm = d.asExtValues(s)) {
      val vs = mdb.valuesForSamplesAndProbes(List(s), ppacked, true, false).flatten
      //In the case of sparse matrices, we may extract additional "absent" values
      //in addition to the ones requested. Hence the subset method is necessary.
      confirm.values.toSet.subsetOf(vs.toSet) should be(true)
    }

    //test valuesforprobe
    for (p <- ppacked.par) {
       val vs = mdb.valuesForProbe(p, sseq)
       val confirm = for (s <- d.samples;
         v <- d.asExtValue(s, probeMap.unpack(p))) yield (s, v)
       
       vs.toSet should equal(confirm.toSet)
    }

    //test valuesinsample with padding
    for (s <- samples.par; cvs = d.asExtValues(s)) {
       val vs = mdb.valuesInSample(s, ppacked, true)
       val confirm = for (p <- ppacked;
         v = cvs.getOrElse(probeMap.unpack(p),
          mdb.emptyValue(probeMap.unpack(p)))
          ) yield v
       vs should equal(confirm)
    }

    //test valuesinsample with no padding
    for (s <- samples.par; cvs = d.asExtValues(s)) {
       val vs = mdb.valuesInSample(s, ppacked, false)
       val confirm = for (p <- ppacked;
         v <- cvs.get(probeMap.unpack(p))) yield v
       vs should equal(confirm)
    }
    //non-contiguous read
    val ss = (0 until 50).map(i => pickOne(samples.toSeq)).distinct
    val ps = (0 until 400).map(i => pickOne(probes)).distinct
    val pset = ps.toSet.map(probeMap.unpack)
    val vs = mdb.valuesForSamplesAndProbes(ss, ps, false, false)
//
//    println("Request s: " + ss)
//    println("Request p: " + ps)

    //Fill in with empty values as needed to get a full matrix
    //for the confirmation data (the db under test should do this
    //for missing values)
    
    val confirm = ps.map(p => {
      val unpacked = probeMap.unpack(p)
        Map() ++ (for { s <- d.samples; v = d.asExtValue(s, unpacked);      
          vv = v.getOrElse(mdb.emptyValue(unpacked)) }
          yield (s, vv))
      })
    
    val confirm2 = confirm.map(row => ss.map(row(_)))

    val c2m = Map() ++ confirm2.map(c => c.head.probe -> c)
    val vm = Map() ++ vs.map(v => v.head.probe -> v)

    for (p <- ppacked.par) {
      val up = probeMap.unpack(p)
      val r1 = c2m.get(up)
      val r2 = vm.get(up)
      if (r2 == None) {
        assert (r1 == None)
      } else {
        if (r1 != r2) {
          for (p <- (r1.get zip r2.get)) {
            println(p._1 + "," + p._2 + "," + (p._1 == p._2) + " " + p._1.probe + "\t")
          }
        }
        r1 should equal(r2)
      }
    }
  }
}
