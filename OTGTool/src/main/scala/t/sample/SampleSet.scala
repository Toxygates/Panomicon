/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.sample

import t.db._
import t.model.sample.Attribute

/**
 * Fundamental query operations for a set of samples.
 * Sample parameters are identified by SampleParameter and values are strings.
 */
trait SampleSet {
  /**
   * The samples in this sample set.
   */
  def samples: Iterable[Sample]

  lazy val sampleIds: Set[SampleId] = samples.map(_.sampleId).toSet

  /**
   * Obtain all available attributes for a given sample.
   */
  def sampleAttributes(sample: Sample): Seq[(Attribute, String)]

  /**
   * Query several sample attributes at once for a given sample.
   * @param querySet the attributes to query, or all if the set is empty.
   */
  def sampleAttributes(sample: Sample,
    querySet: Iterable[Attribute]): Seq[(Attribute, String)] = {
    val qs = querySet.toSet
    val ps = sampleAttributes(sample)
    if (querySet.isEmpty)
      ps
    else
      ps.filter(p => qs.contains(p._1))
  }

  /**
   * Query a specific attribute for a given sample.
   */
  def sampleAttribute(sample: Sample, attrib: Attribute): Option[String] =
    sampleAttributes(sample, Seq()).find(_._1 == attrib).map(_._2)

  @deprecated("Query by Attribute instead.", "June 2017")
  def parameter(sample: Sample, parameter: String): Option[String] =
    sampleAttributes(sample, Seq()).find(_._1.id == parameter).map(_._2)

}
