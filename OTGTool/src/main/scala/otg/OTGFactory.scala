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

package otg

import otg.db.Metadata
import otg.sparql.OTGSamples
import otg.sparql.OTGProbes
import t.BaseConfig
import t.DataConfig

import t.TriplestoreConfig
import t.db.file.MapMetadata
import t.model.sample.AttributeSet
import t.sparql._
import t.model.sample.Attribute

class OTGFactory extends t.Factory {
  override def samples(config: BaseConfig): OTGSamples =
    new OTGSamples(config)

  override def probes(config: TriplestoreConfig): OTGProbes =
    new OTGProbes(config)

  override def context(ts: TriplestoreConfig, data: DataConfig) = {
    val bc = new OTGBConfig(ts, data)
    otg.OTGContext(bc)
  }

  override def metadata(data: Map[String, Seq[String]], attr: AttributeSet): Metadata =
    new MapMetadata(data, attr) with otg.db.Metadata

  override def triplestoreMetadata(samples: Samples, attributeSet: AttributeSet,
      querySet: Iterable[Attribute] = Seq())
      (implicit sf: SampleFilter): TriplestoreMetadata =
    new TriplestoreMetadata(samples, attributeSet, querySet)(sf) with otg.db.Metadata

  override def cachingTriplestoreMetadata(samples: Samples, attributeSet: AttributeSet,
      querySet: Iterable[Attribute] = Seq())
      (implicit sf: SampleFilter): CachingTriplestoreMetadata =
    new CachingTriplestoreMetadata(samples, attributeSet, querySet)(sf) with otg.db.Metadata
}
