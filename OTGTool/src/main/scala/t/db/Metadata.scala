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

import t.Factory
import t.model.sample.CoreParameter._
import t.sample.SampleSet
import t.model.sample.{Attribute, AttributeSet, OTGAttribute}

trait Metadata extends SampleSet {
  def samples: Iterable[Sample]

  def attributeSet: AttributeSet

  def parameterMap(s: Sample): Map[String, String] =
    Map() ++ sampleAttributes(s).map(x => x._1.id -> x._2)

  /**
   * Obtain all values for a given parameter represented by the samples in
   * this metadata.
   */
  def attributeValues(attr: Attribute): Seq[String]

  override def parameter(s: Sample, identifier: String): Option[String] =
    parameterMap(s).get(identifier)

  /**
   * Does this metadata set have information about the given sample?
   */
  def contains(s: Sample): Boolean = !sampleAttributes(s).isEmpty

  def platform(s: Sample): String = parameter(s, "platform_id").get

  def isControl(s: Sample): Boolean = sampleAttribute(s, Treatment) == sampleAttribute(s, ControlTreatment)

  /**
   * Obtain a new metadata set after applying a mapping function to one
   * of the parameters.
   */
  def mapParameter(fact: Factory, key: String, f: String => String): Metadata

  private def controlGroupKey(s: Sample) = sampleAttribute(s, ControlTreatment).get

  def treatedControlGroups(ss: Iterable[Sample]): List[(List[Sample], List[Sample])] = {
    ss.groupBy(controlGroupKey(_)).toList.map { case (key, allSamples) => {
      allSamples.toList.partition(!isControl(_))
    } }
  }
}

/**
 * A limited view of a larger metadata set, where only some samples are visible.
 * Note: the filtering is intended to keep control units intact, so filters should not
 * split such units in two. 
 */
class FilteredMetadata(from: Metadata, visibleSamples: Iterable[Sample]) extends Metadata {
  val samples = visibleSamples.toSet
  
  def attributeSet = from.attributeSet
  
  def attributeValues(attr: Attribute): Seq[String] =
    samples.toSeq.flatMap(x => sampleAttributes(x, Seq(attr)).map(_._2)).distinct
  
  def mapParameter(fact: Factory, key: String, f: String => String): Metadata =
    new FilteredMetadata(from.mapParameter(fact, key, f), visibleSamples)

  def sampleAttributes(sample: Sample): Seq[(Attribute, String)] =
    if (samples.contains(sample)) from.sampleAttributes(sample) else Seq()
    
  override def contains(s: Sample) = samples.contains(s)
}
