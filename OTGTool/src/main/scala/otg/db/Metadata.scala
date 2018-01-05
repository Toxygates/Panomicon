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

import t.db.ParameterSet
import t.db.Sample
import otg.model.sample.OTGAttribute._
import t.model.sample.CoreParameter._

/**
 * Information about a set of samples.
 */
trait Metadata extends t.db.Metadata {

  def attributeSet: t.model.sample.AttributeSet

  override def isControl(s: Sample): Boolean = sampleAttribute(s, DoseLevel).get == "Control"

  private def controlGroupKey(s: Sample) =
    (sampleAttribute(s, ControlGroup), sampleAttribute(s, ExposureTime), sampleAttribute(s, Batch))

  override def controlSamples(s: Sample): Iterable[Sample] = {
    val key = controlGroupKey(s)
    samples.filter(controlGroupKey(_) == key).filter(isControl)
  }

  override def treatedControlGroups(ss: Iterable[Sample]) = {
    val gs = super.treatedControlGroups(ss)
    gs.flatMap({
      case (treated, control) => {
        treated.groupBy(_.get(DoseLevel)).values.toSeq.map(ts => (ts, control))
      }
    })
  }
}
