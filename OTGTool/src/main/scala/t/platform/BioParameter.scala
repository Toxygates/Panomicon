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

package t.platform

import t.db.Sample
import org.apache.commons.math3.stat.StatUtils
import t.sample.SampleSet
import t.model.sample.{Attribute, VarianceSet}

/**
 * Construct a BioParameter object.
 * @param attributes attributes of the bio parameter (not sample attributes) -
 *  tentative functionality for describing bio parameters in platforms
 */
case class BioParameter(attribute: Attribute,
    section: Option[String],
    lowerBound: Option[Double], upperBound: Option[Double],
    attributes: Map[String, String] = Map()) {

  def key: String = attribute.id

  def label: String = attribute.title

  def kind: String = {
    if (attribute.isNumerical()) "numerical" else "text"
  }
}

class BioParameters(lookup: Map[Attribute, BioParameter]) {
  def apply(key: Attribute): BioParameter = lookup(key)
  def get(key: Attribute): Option[BioParameter] = lookup.get(key)

  /**
   * Obtain the set as attributes, sorted by section and label.
   */
  def sampleParameters: Seq[Attribute] = lookup.values.toSeq.
    sortBy(p => (p.section, p.label)).map(_.attribute)

  /**
   * Extract bio parameters with accurate low and high threshold for a given
   * time point. The raw values are stored in the root parameter set's
   * attribute maps.
   * @param time The time point, e.g. "24 hr"
   */
  def forTimePoint(time: String): BioParameters =
    new BioParameters(lookup)

  def all: Iterable[BioParameter] = lookup.values
}

/**
 * A VarianceSet that retrieves attribute values from a SampleSet
 */
class SSVarianceSet(sampleSet: SampleSet, val samples: Iterable[Sample]) extends VarianceSet {
  val paramVals = samples.map(Map() ++ sampleSet.sampleAttributes(_))

  def standardDeviation(attribute: Attribute): java.lang.Double = {
    val nvs = paramVals.flatMap(_.get(attribute)).
      flatMap(BioParameter.tryParseDouble)
    if (nvs.size < 2) {
      null
    } else {
      Math.sqrt(StatUtils.variance(nvs.toArray))
    }
  }

  def mean(attribute: Attribute): java.lang.Double = {
    val nvs = paramVals.flatMap(_.get(attribute)).
      flatMap(BioParameter.tryParseDouble)
    if (nvs.size < 2) {
      null
    } else {
      StatUtils.mean(nvs.toArray)
    }
  }
}

object BioParameter {
  def tryParseDouble(x: String): Option[Double] = x.toLowerCase match {
      case Attribute.NOT_AVAILABLE => None
      case Attribute.UNDEFINED_VALUE | Attribute.UNDEFINED_VALUE_2 => None
      case _    => Some(x.toDouble)
    }
}
