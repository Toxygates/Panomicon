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

package t.testing

import t.db.Metadata
import otg.Species._
import t.db.ProbeMap
import t.db.SampleMap
import t.db.MatrixContext
import t.db.MatrixDBReader
import t.db.PExprValue
import t.db.ExprValue
import t.db.SeriesBuilder
import t.db.testing.TestData
import t.db.kyotocabinet.KCMatrixDB
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.MatrixDB
import t.db.ExtMatrixDB
import t.db.SeriesDB

class FakeContext(val sampleMap: SampleMap, val probeMap: ProbeMap,
  val enumMaps: Map[String, Map[String, Int]] = Map()) extends MatrixContext {
  import TestData._

  def species = List(Rat)

  def probes(s: Species): ProbeMap = probeMap
  def unifiedProbes = probeMap

  /**
   * Metadata for samples. Not always available.
   * Only intended for use during maintenance operations.
   */
  def metadata: Option[Metadata] = None

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
