/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import t.{OTGDoseSeriesBuilder, OTGTimeSeriesBuilder}
import t.platform.Species._
import t.db._
import t.db.kyotocabinet.KCSeriesDB
import t.db.testing.DBTestData._
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.db.testing.DBTestData

class FakeContext(val sampleMap: SampleMap, val probeMap: ProbeMap) extends MatrixContext {
  def this() { this(DBTestData.sampleMap, DBTestData.probeMap) }

  val enumMaps: Map[String, Map[String, Int]] = DBTestData.enumMaps

  def species = List(Rat)

  def probes(s: Species): ProbeMap = probeMap
  def unifiedProbes = probeMap

  def samples = ???

  val testData = makeTestData(true)

  private val folds = memDBHash
  private val abs = memDBHash

  lazy val absoluteDBReader: ExtMatrixDBReader = ???
  lazy val foldsDBReader: ExtMatrixDB = new KCChunkMatrixDB(folds, false)(this)

  val timeSeriesDB = t.db.testing.DBTestData.memDBHash
  val doseSeriesDB = t.db.testing.DBTestData.memDBHash

  def timeSeriesBuilder = OTGTimeSeriesBuilder
  def timeSeriesDBReader = new KCSeriesDB(timeSeriesDB, false, timeSeriesBuilder, true)(this)
  def doseSeriesBuilder = OTGDoseSeriesBuilder
  def doseSeriesDBReader = new KCSeriesDB(doseSeriesDB, false, doseSeriesBuilder, true)(this)

  override def expectedProbes(x: Sample) = {
    probeMap.keys.toSeq
  }

  def populate() {
    populate(testData)
  }

  def populate(d: ColumnExpressionData) {
    DBTestData.populate(foldsDBReader, d)(probeMap)
  }
}
