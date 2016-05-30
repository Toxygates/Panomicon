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

package otg.testing

import t.db.SampleMap
import t.db.ProbeMap
import otg.OTGSeries
import t.db.kyotocabinet.KCSeriesDB
import t.db.testing.{TestData => TData}

class FakeContext(sampleMap: SampleMap = TData.dbIdMap,
    probeMap: ProbeMap = TData.probeMap,
    enumMaps: Map[String, Map[String, Int]] = TData.enumMaps)
  extends t.testing.FakeContext(sampleMap, probeMap, enumMaps) {

  val seriesDB = t.db.testing.TestData.memDBHash
  override def seriesBuilder = OTGSeries
  override def seriesDBReader = new KCSeriesDB(seriesDB, false, seriesBuilder, true)(this)
}
