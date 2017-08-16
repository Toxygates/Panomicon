/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.testing

import otg.Species._
import t.db._
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.kyotocabinet.KCMatrixDB
import t.db.testing.TestData

class FakeContext(val sampleMap: SampleMap, val probeMap: ProbeMap,
  val enumMaps: Map[String, Map[String, Int]] = Map(),
  val metadata: Option[Metadata] = None) extends MatrixContext {
  import TestData._

  def species = List(Rat)

  def probes(s: Species): ProbeMap = probeMap
  def unifiedProbes = probeMap

  def samples = ???

  val testData = makeTestData(true)

  private val folds = memDBHash
  private val abs = memDBHash

  lazy val absoluteDBReader: KCMatrixDB = ???
  lazy val foldsDBReader: KCExtMatrixDB = new KCExtMatrixDB(folds, false)

  def seriesDBReader: SeriesDB[_] = ???
  def seriesBuilder: SeriesBuilder[_] = ???

  def populate() {
    TestData.populate(foldsDBReader, testData)
  }
}
