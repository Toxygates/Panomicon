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

package t.db

import t.model.sample.{Attribute, VarianceSet}
import org.apache.commons.math3.stat.StatUtils

import scala.collection.mutable.HashMap

/**
 * VarianceSet that simply uses the parameter values stored in a sample object
 */
class SimpleVarianceSet(samples: Iterable[t.model.sample.SampleLike]) extends VarianceSet {

  val standardDeviationsAndMeans = new HashMap[Attribute, Option[(Double, Double)]]

  def standardDeviation(attribute: Attribute): java.lang.Double = {
    ensureComputed(attribute)
    standardDeviationsAndMeans.get(attribute) match {
      case Some(Some((standardDeviation, mean))) => standardDeviation
      case _ => null
    }
  }

  def mean(attribute: Attribute): java.lang.Double = {
    ensureComputed(attribute)
    standardDeviationsAndMeans.get(attribute) match {
      case Some(Some((standardDeviation, mean))) => mean
      case _ => null
    }
  }

  def ensureComputed(attribute: Attribute): Unit = {
    standardDeviationsAndMeans.get(attribute) match {
      case None =>
        val attributeValues = samples.flatMap(Sample.numericalValue(_, attribute)).toArray
        val newPair = if (attributeValues.size < 2) {
          None
        } else {
          Some((Math.sqrt(StatUtils.variance(attributeValues)),
            StatUtils.mean(attributeValues)))
        }
        standardDeviationsAndMeans.put(attribute, newPair)
      case _ =>
    }
  }
}
