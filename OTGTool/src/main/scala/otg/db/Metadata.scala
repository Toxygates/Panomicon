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

package otg.db

import scala.collection.immutable.ListMap

import t.db.ParameterSet
import t.db.Sample
import otg.model.sample.Attribute._
import t.model.sample.CoreParameter._

/**
 * Information about a set of samples.
 */
trait Metadata extends t.db.Metadata {

  def attributes: t.model.sample.AttributeSet
  
  override def isControl(s: Sample): Boolean = parameter(s, DoseLevel).get == "Control"

}

object OTGParameterSet extends ParameterSet {

  /**
   * Find the files that are control samples in the collection that a given barcode
   * belongs to.
   *
   * TODO factor out commonalities with SparqlServiceImpl
   */
  override def controlSamples(metadata: t.db.Metadata, s: Sample): Iterable[Sample] = {
    val expTime = metadata.parameter(s, ExposureTime)
    val cgroup = metadata.parameter(s, ControlGroup)

    println(expTime + " " + cgroup)
    metadata.samples.filter(s => {
      metadata.parameter(s, ExposureTime) == expTime &&
      metadata.parameter(s, ControlGroup) == cgroup &&
      metadata.isControl(s)
    })
  }

  override def treatedControlGroups(metadata: t.db.Metadata, ss: Iterable[Sample]):
    Iterable[(Iterable[Sample], Iterable[Sample])] = {
    for (
      (cs, ts) <- ss.groupBy(controlSamples(metadata, _));
      (d, dts) <- ts.groupBy(metadata.parameter(_, DoseLevel))
    ) yield ((dts, cs))
  }
}
