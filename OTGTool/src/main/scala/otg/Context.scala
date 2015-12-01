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

package otg

import otg.db.Metadata
import otg.sparql.OTGSamples
import t.TriplestoreConfig
import t.db.MatrixContext
import t.db.MatrixDBReader
import t.db.ProbeIndex
import t.db.kyotocabinet.KCMatrixDB
import t.db.SampleIndex
import t.BaseConfig
import t.db.PExprValue
import t.db.ExprValue
import t.db.ProbeMap
import t.sparql.Samples
import otg.sparql.Probes
import t.db.TRefresher
import t.db.SeriesBuilder
import t.db.TransformingWrapper
import t.db.BasicExprValue

object Context {
  val factory = new Factory()

  def apply(bc: OTGBConfig) =
    new otg.Context(bc, factory,
      factory.probes(bc.triplestore), factory.samples(bc),
      new OTGContext(bc, None))
}

//TODO consider adding a sparql subcontext with lazy val endpoints
class Context(override val config: OTGBConfig,
  override val factory: Factory,
  override val probes: Probes,
  override val samples: OTGSamples,
  override val matrix: OTGContext)
  extends t.Context(config, factory, probes, samples, matrix)

/**
 * TODO: split up properly/rename
 */
class OTGContext(baseConfig: BaseConfig,
  /**
   * Metadata for samples. Not always available.
   * Only intended for use during maintenance operations.
   */
  val metadata: Option[Metadata] = None) extends MatrixContext {

  private val data = baseConfig.data
  private val maps = new TRefresher(baseConfig)

  def triplestoreConfig: TriplestoreConfig = baseConfig.triplestore

  def probeMap = maps.latest.unifiedProbes

  def enumMaps = maps.latest.enumMaps
  def sampleMap = maps.latest.sampleMap

  lazy val samples: OTGSamples =
    new OTGSamples(baseConfig)

  /**
   * Obtain a reader for the absolute value/normalized intensity
   *  database.
   */
  def absoluteDBReader: MatrixDBReader[ExprValue] =
    data.absoluteDBReader(this)

  /**
   * Obtain a reader for the folds database.
   */
  def foldsDBReader: MatrixDBReader[PExprValue] =
    data.foldsDBReader(this)

  def seriesBuilder: OTGSeries.type = OTGSeries

  //TODO add series DB reader

  var testRun: Boolean = false

  def close() {
    samples.close()
  }
}
