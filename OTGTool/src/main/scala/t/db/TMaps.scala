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

package t.db

import t.BaseConfig
import t.Refreshable
import t.db.kyotocabinet.KCIndexDB

//TODO: this is arguably a KyotoCabinet specific concept.
class TMaps(config: BaseConfig) {
  def data = config.data

  lazy val unifiedProbes: ProbeMap =
    new ProbeIndex(KCIndexDB.readOnce(data.probeIndex))

  lazy val sampleMap =
    new SampleIndex(KCIndexDB.readOnce(data.sampleIndex))

  lazy val enumMaps = {
    val db = KCIndexDB(data.enumIndex, false)
    try {
      db.enumMaps(config.seriesBuilder.enums)
    } finally {
      db.release
    }
  }
}

class TRefresher(config: BaseConfig) extends Refreshable[TMaps] {

  def currentTimestamp: Long = {
    //If this file has been updated, we reload all the maps
    val files = List(config.data.sampleDb, config.data.probeDb, config.data.enumDb)
    files.map(new java.io.File(_).lastModified()).max
  }

  def reload: TMaps = new TMaps(config)
}
