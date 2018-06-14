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

package t.db.kyotocabinet.chunk

import t.TTestSuite
import t.db.kyotocabinet.KCDBTest
import t.db.testing.TestData
import t.db.PExprValue
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Gen
import org.scalacheck._
import org.scalacheck.Prop._
import Arbitrary.arbitrary
import org.scalatest.prop.Checkers

@RunWith(classOf[JUnitRunner])
class KCChunkMatrixDBTest extends TTestSuite with Checkers {
  import KCDBTest._
  import TestData._
  import KCChunkMatrixDB._

  test("basic") {
    val db = memDBHash
    val edb = new KCChunkMatrixDB(db, true)

    testExtDb(edb, makeTestData(false))
    edb.release
  }

  test("sparse data") {
    val db = memDBHash
    val edb = new KCChunkMatrixDB(db, true)

    testExtDb(edb, makeTestData(true))
    edb.release
  }
  
  type VC = VectorChunk[PExprValue]
  
  def mkValues(start: Int, n: Int): Seq[PExprValue] = 
    (start until (start + n)).map(i => randomPExpr(probeMap.unpack(i)))
  def mkValues(n: Int): Seq[PExprValue] = mkValues(0, n)
  
  val genPExprValues: Gen[Seq[(Int, PExprValue)]] = for {
    size <- Gen.choose(1, 100)
    factor <- Gen.choose(0, 3)
    start = factor * CHUNKSIZE
    end = start + size
    data = (start until end) zip mkValues(start, size)
  } yield data
   
  
  def genChunk: Gen[VC] = for {    
    sample <- arbitrary[Int]
    data <- genPExprValues
    startProbe = data.head._1
  } yield new VectorChunk[PExprValue](sample, startProbe, data)
  
  implicit def arbChunk = Arbitrary(genChunk)
  
  test("Vector chunk props") {
     check { (c:VC) => c.probes.size == c.xs.size }
     check { (c:VC) =>       
       (c.probes).forall( n => 
         (c.remove(n).xs.size == c.xs.size - 1)
         )
      }
  }
  
  test("Vector Chunk") {

    var vc = new VectorChunk[PExprValue](0, 0, Seq())
    val xs = (mkValues(500).zipWithIndex)

    val p1 = xs.take(100)
    for (x <- p1) {
      vc = vc.insert(x._2, x._1)
    }
    vc.xs.size should equal(100)
    vc.probes should equal((0 until 100))

    vc = vc.remove(49)
    vc = vc.remove(19)
    vc.xs.size should equal(98)
    val valid = (p1.filter(x => x._2 != 49 && x._2 != 19))
    vc.xs.map(x => (x._2, x._1)) should equal(valid)

    vc = vc.insert(xs(10)._2, xs(10)._1)
    vc = vc.insert(xs(30)._2, xs(30)._1)
    vc.xs.map(x => (x._2, x._1)) should equal(valid)

  }
}
