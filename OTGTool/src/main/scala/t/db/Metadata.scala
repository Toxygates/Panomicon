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

package t.db

import friedrich.util.formats.TSVFile
import t.Factory
import t.sample.SampleSet
import t.model.sample.Attribute
import t.model.sample.AttributeSet

trait ParameterSet {
  /**
   * Retrieve the set of control samples corresponding to a given sample.
   */
  def controlSamples(metadata: Metadata, s: Sample): Iterable[Sample] = Seq()

  /**
   * Compute groups of treated and control samples for p-value computation.
   * This is a naive implementation which needs to be overridden if control samples are
   * shared between multiple treated groups.
   */
  def treatedControlGroups(metadata: Metadata, ss: Iterable[Sample]): Iterable[(Iterable[Sample], Iterable[Sample])] = {
    ss.groupBy(controlSamples(metadata, _)).toSeq.map(sg => {
      sg._2.partition(!metadata.isControl(_))
    })
  }
}

trait Metadata extends SampleSet {
  def samples: Iterable[Sample]

  def attributeSet: AttributeSet

  def parameterMap(s: Sample): Map[String, String] =
    Map() ++ sampleAttributes(s).map(x => x._1.id -> x._2)
  /**
   * Obtain all available values for a given parameter.
   */
  def parameterValues(identifier: String): Set[String]

  def attributeValues(attr: Attribute): Set[String] =
    parameterValues(attr.id)

  override def parameter(s: Sample, identifier: String): Option[String] =
    parameterMap(s).get(identifier)

  /**
   * Does this metadata set have information about the given sample?
   */
  def contains(s: Sample): Boolean = !sampleAttributes(s).isEmpty

  def platform(s: Sample): String = parameter(s, "platform_id").get

  def isControl(s: Sample): Boolean = false

  /**
   * Obtain a new metadata set after applying a mapping function to one
   * of the parameters.
   */
  def mapParameter(fact: Factory, key: String, f: String => String): Metadata

  def controlSamples(s: Sample): Iterable[Sample] = ???

  def treatedControlGroups(ss: Iterable[Sample]): Iterable[(Iterable[Sample], Iterable[Sample])] = {
    ss.groupBy(controlSamples(_)).toSeq.map(sg => {
      sg._2.partition(!isControl(_))
    })
  }
}
