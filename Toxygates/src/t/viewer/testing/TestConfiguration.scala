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

package t.viewer.testing

import t.viewer.server.Configuration
import t.db.kyotocabinet.chunk.KCChunkMatrixDB

/**
 * Generates configurations for testing of servlet implementations.
 * Maps the TestConfig from OTGTool into a Configuration.
 */
object TestConfiguration {
  val tc = t.testing.TestConfig
  val ts = tc.tsConfig
  val data = tc.dataConfig

  lazy val config = new Configuration(ts.repository,
      KCChunkMatrixDB.CHUNK_PREFIX + data.dir, null, null,
      ts.url, ts.updateUrl,
      ts.user, ts.pass,
      null, null,
      data.matrixDbOptions)

}
