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

import t.db.MemoryLookupMap
import t.db.ProbeMap
import t.db.Sample
import t.db.BasicExprValue
import t.db.MatrixContext
import t.db.SampleMap

object TestData extends MatrixContext {
  val probeIds = (1 to 100)

  implicit val probeMap = {
    val pmap = Map() ++ probeIds.map(x => ("probe_" + x -> x))
    new MemoryLookupMap(pmap) with ProbeMap
  }

  val probes = probeIds.map(probeMap.unpack)

  val samples = (1 to 100).map(s => Sample(s"x$s"))

  val sampleMap = {
    val samples = Map() ++ (1 to 100).map(s => (s"x$s" -> s))
    new MemoryLookupMap(samples) with SampleMap
  }

  lazy val exprRecords = for (
    p <- probeMap.keys.toSeq; s <- samples;
    v = Math.random() * 100
  ) yield (s, p, BasicExprValue(v, 'P', probeMap.unpack(p)))

  lazy val matrix = new FakeBasicMatrixDB(exprRecords)

  def enumMaps = Map()

  def absoluteDBReader = matrix
  def foldsDBReader = { throw new Exception("Implement me") }
  def seriesBuilder = { throw new Exception("Implement me") }
}
