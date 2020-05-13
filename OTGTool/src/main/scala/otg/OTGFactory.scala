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

import t.db.Metadata
import t.BaseConfig
import t.DataConfig
import t.TriplestoreConfig
import t.db.file.MapMetadata
import t.model.sample.AttributeSet
import t.sparql.{SampleStore, ProbeStore, _}
import t.model.sample.Attribute
import t.db.FilteredMetadata
import t.db.Sample

class OTGFactory extends t.Factory {
  override def samples(config: BaseConfig): SampleStore =
    new SampleStore(config)

  override def probes(config: TriplestoreConfig): ProbeStore =
    new ProbeStore(config)

  override def context(ts: TriplestoreConfig, data: DataConfig) = {
    val bc = new OTGBConfig(ts, data)
    otg.OTGContext(bc)
  }

  override def metadata(data: Map[String, Seq[String]], attr: AttributeSet): Metadata =
    new MapMetadata(data, attr) with t.db.Metadata

  override def triplestoreMetadata(sampleStore: SampleStore, attributeSet: AttributeSet,
                                   querySet: Iterable[Attribute] = Seq())
      (implicit sf: SampleFilter): TriplestoreMetadata =
    new TriplestoreMetadata(sampleStore, attributeSet, querySet)(sf) with t.db.Metadata

  override def cachingTriplestoreMetadata(sampleStore: SampleStore, attributeSet: AttributeSet,
                                          querySet: Iterable[Attribute] = Seq())
      (implicit sf: SampleFilter): CachingTriplestoreMetadata =
    new CachingTriplestoreMetadata(sampleStore, attributeSet, querySet)(sf) with t.db.Metadata
    
  override def filteredMetadata(from: t.db.Metadata, sampleView: Iterable[Sample]) =
    new FilteredMetadata(from, sampleView) with t.db.Metadata
}
