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

package t

import otg.sparql.OTGSamples
import t.db.Metadata
import t.db.Sample
import t.sparql.Samples

abstract class ValueSummary(valueName: String) {
  def originalValues: Set[String]

  var newValues: Set[String] = Set()
  var existingValues: Set[String] = Set()

  def check(x: String) {
    if (originalValues.contains(x)) {
      existingValues += x
    } else {
      newValues += x
    }
  }

  def summary(detail: Boolean): String = {
    var r = valueName + ": " + existingValues.size + " existing values, " +
      newValues.size + " new values"
    if (detail && newValues.size > 0) {
      r += ": " + newValues.mkString(" ")
    }
    r
  }
}

class SimpleValueSummary(valueName: String, val originalValues: Set[String])
  extends ValueSummary(valueName)

case class AttribValueSummary(samples: Samples, attribName: String)
  extends ValueSummary(attribName) {
  val originalValues = samples.allValuesForSampleAttribute(attribName).toSet

  def check(md: Metadata, samples: Iterable[Sample]) {
    for (
      s <- samples; a <- md.parameters(s);
      if (a._1.identifier == attribName)
    ) {
      check(a._2)
    }
  }
}
