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

package otg

import t.db._
import t.db.kyotocabinet.KCSeriesDB
import t.sparql.{SampleStore, ProbeStore}
import t.{BaseConfig, OTGDoseSeriesBuilder, OTGTimeSeriesBuilder, TriplestoreConfig}

object OTGContext {
  val factory = new OTGFactory()

  def apply(bc: OTGBConfig) =
    new otg.OTGContext(bc, factory,
      factory.probes(bc.triplestore), factory.samples(bc),
      new OTGMatrixContext(bc))
}

class OTGContext(override val config: OTGBConfig,
                 override val factory: OTGFactory,
                 override val probeStore: ProbeStore,
                 override val sampleStore: SampleStore,
                 override val matrix: OTGMatrixContext)
  extends t.Context(config, factory, probeStore, sampleStore, matrix)

class OTGMatrixContext(baseConfig: BaseConfig) extends MatrixContext {

  private val data = baseConfig.data
  private val maps = new TRefresher(baseConfig)

  def triplestoreConfig: TriplestoreConfig = baseConfig.triplestore

  def probeMap = maps.latest.unifiedProbes

  def enumMaps = maps.latest.enumMaps
  def sampleMap = maps.latest.sampleMap

  lazy val samples: SampleStore =
    new SampleStore(baseConfig)

  /**
   * Obtain a reader for the absolute value/normalized intensity
   *  database.
   */
  def absoluteDBReader: MatrixDBReader[PExprValue] =
    data.absoluteDBReader(this)

  /**
   * Obtain a reader for the folds database.
   */
  def foldsDBReader: MatrixDBReader[PExprValue] =
    data.foldsDBReader(this)

  def timeSeriesBuilder: OTGTimeSeriesBuilder.type = OTGTimeSeriesBuilder
  def doseSeriesBuilder: OTGDoseSeriesBuilder.type = OTGDoseSeriesBuilder

  def timeSeriesDBReader: SDB =
    KCSeriesDB(data.timeSeriesDb, false, timeSeriesBuilder, true)(this)
  def doseSeriesDBReader: SDB =
    KCSeriesDB(data.doseSeriesDb, false, doseSeriesBuilder, true)(this)

  var testRun: Boolean = false

  def close() {
    samples.close()
  }
}
