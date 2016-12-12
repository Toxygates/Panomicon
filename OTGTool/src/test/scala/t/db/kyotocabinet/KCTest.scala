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

package t.db.kyotocabinet

import t.db.ExprValue
import otg.OTGContext
import t.TTestSuite
import t.db.MatrixDBReader
import otg.Species._
import t.testing.TestConfig
import t.db.Sample

class KCTest extends TTestSuite {
  var db: MatrixDBReader[ExprValue] = _

  val config = TestConfig.config
  implicit val context = new OTGContext(config)
  val pmap = context.probeMap

  before {
    db = context.absoluteDBReader
  }

  after {
    db.release
  }

  val nprobes = 130692 //mouse + rat + human

  test("Read data") {
    val s = Sample("003017629005")
    println(s.dbCode)
    println(pmap.keys.size + " probes known")
    println(pmap.keys.take(5))
    println(context.sampleMap.tokens.size + " samples known")

    println(context.sampleMap.tokens.take(5))
    val r = db.valuesInSample(s, List())
    r.size should equal(nprobes)
  }

  test("Read by probe and barcode") {
    // Verify that data read probe-wise equals data read barcode-wise
    val ss = db.sortSamples(List("003017629001", "003017629013", "003017629014").map(Sample(_)))
    val r1 = db.valuesInSamples(ss, List())
    r1.toVector.flatten.size should equal(nprobes * 3)

    val ps = List("1371970_at", "1371034_at", "1372727_at")
    val packedProbes = ps.map(pmap.pack(_))
    val r1f = r1.map(_.filter(x => ps.contains(x.probe))).toSeq
    val pps = ps.zip(ps.map(pmap.pack(_)))
    val r2s = pps.map(x => db.valuesForProbe(x._2, ss))
    for (i <- 0 until ss.size) {
      val r1ForSample = r1f(i).toSet
      val r2ForSample = r2s.flatten.filter(_._1 == ss(i)).map(_._2).toSet
      r1ForSample should equal(r2ForSample)
      println(r1ForSample + " == " + r2ForSample)
    }
  }
}
