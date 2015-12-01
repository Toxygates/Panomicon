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

package t.db.testing

import t.db.PExprValue
import t.db.ProbeIndex
import t.db.RawExpressionData
import t.db.Sample
import t.db.SampleIndex
import t.testing.FakeContext

object TestData {
  def pickOne[T](xs: Seq[T]): T = {
    val n = Math.random * xs.size
    xs(n.toInt)
  }

  def calls = List('A', 'P', 'M')

  val ids = (0 until 96).toStream.iterator
  val samples = for (
    dose <- Set("Control", "High"); time <- Set("3hr", "24hr");
    ind <- Set("1", "2", "3"); compound <- Set("Chocolate", "Tea", "Cocoa", "Irish");
    s = Sample("s" + ids.next, Map("dose_level" -> dose, "individual_id" -> ind,
      "exposure_time" -> time, "compound_name" -> compound))
  ) yield s

  def randomExpr(): (Double, Char, Double) = {
    val v = Math.random * 100000
    val call = pickOne(calls)
    (v, call, Math.abs(Math.random))
  }

  def randomPExpr(probe: String): PExprValue = {
    val v = randomExpr
    new PExprValue(v._1, v._3, v._2, probe)
  }

  val probes = (0 until 500)

  implicit val probeMap = {
    val pmap = Map() ++ probes.map(x => ("probe_" + x -> x))
    new ProbeIndex(pmap)
  }

  val dbIdMap = {
    val dbIds = Map() ++ samples.zipWithIndex.map(s => (s._1.sampleId -> s._2))
    new SampleIndex(dbIds)
  }

  implicit val context = new FakeContext(dbIdMap, probeMap)

  def makeTestData(sparse: Boolean): RawExpressionData = {
    var testData = Map[Sample, Map[String, (Double, Char, Double)]]()
    for (s <- samples) {
      var thisProbe = Map[String, (Double, Char, Double)]()
      for (p <- probeMap.tokens) {
        if (!sparse || Math.random > 0.5) {
          thisProbe += (p -> randomExpr())
        }
      }
      testData += (s -> thisProbe)
    }
    new RawExpressionData {
      val data = testData
    }
  }

}
