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

package otg.db.file

import friedrich.util.formats.TSVFile
import otg.db.Metadata
import scala.Array.canBuildFrom
import t.db.Sample
import otg.db.OTGParameterSet

/**
 * Represents a metadata file of the type used in Open TG-Gates.
 * A file with tab-separated values.
 */
class TSVMetadata(file: String) extends t.db.file.TSVMetadata(file, OTGParameterSet) with otg.db.Metadata {

  /**
   * Find the files that are control samples in the collection that a given barcode
   * belongs to.
   *
   * TODO think about ways to generalise this without depending on the
   * key/value of dose_level = Control. Possibly depend on DataSchema
   */
  override def controlSamples(s: Sample): Iterable[Sample] = {
    val idx = getIdx(s)
    val expTime = metadata("exposure_time")(idx)
    val cgroup = metadata("control_group")(idx)

    println(expTime + " " + cgroup)

    val idxs = (0 until metadata("sample_id").size).filter(x => {
      metadata("exposure_time")(x) == expTime &&
        metadata("control_group")(x) == cgroup &&
        metadata("dose_level")(x) == "Control"
    })
    idxs.map(metadata("sample_id")(_)).map(Sample(_))
  }

}
