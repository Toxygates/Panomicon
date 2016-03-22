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

package t
import t.sparql._
import t.db.file.TSVMetadata
import t.db.ParameterSet
import t.db.kyotocabinet.chunk.KCChunkMatrixDB

//TODO consider making ts, data constructor parameters
//then store them and the resulting context, baseconfig
abstract class Factory {
  def samples(config: BaseConfig): Samples

  def probes(config: TriplestoreConfig): Probes

  def tsvMetadata(file: String): TSVMetadata

  def context(ts: TriplestoreConfig, data: DataConfig): Context

  def dataConfig(dir: String, matrixDbOptions: String): DataConfig =
    DataConfig.apply(dir, matrixDbOptions)
}
