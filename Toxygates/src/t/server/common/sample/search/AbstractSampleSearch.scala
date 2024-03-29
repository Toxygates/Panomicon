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

package t.server.common.sample.search

import scala.collection.JavaConverters._
import scala.collection.Seq
import t.shared.common.DataSchema
import t.shared.common.sample.Sample
import t.shared.common.sample.search.AndMatch
import t.shared.common.sample.search.AtomicMatch
import t.shared.common.sample.search.MatchCondition
import t.shared.common.sample.search.MatchType
import t.shared.common.sample.search.OrMatch
import t.model.SampleClass
import t.model.sample.{Attribute, AttributeSet, CoreParameter, SampleLike, VarianceSet}
import t.sparql.SampleClassFilter
import t.sparql.SampleFilter
import t.sparql.SampleStore
import t.server.viewer.Conversions.asJavaSample
import t.server.viewer.UnitsHelper
import t.shared.viewer.OTGSchema

  /**
   * Companion object to create sample search objects; meant to encapsulate
   * some initialization logic required in all sample search subclasses.
   * @tparam ST the type of objects that SS searches through
   * @tparam SS the type of SampleSearch object to be created
   */
trait SearchCompanion[ST <: SampleLike, SS <: AbstractSampleSearch[ST]] {

  protected def rawSamples(condition: MatchCondition, sampleClass: SampleClass,
                           sampleFilter: SampleFilter, sampleStore: SampleStore,
                           attributes: AttributeSet): Seq[Sample] = {

    val queryAttributes = condition.neededParameters().asScala ++
      attributes.getUnitLevel().asScala ++ Seq(CoreParameter.ControlGroup)
    sampleStore.sampleAttributeQuery(queryAttributes, sampleFilter, SampleClassFilter(sampleClass))().map(asJavaSample)
  }

  def apply(condition: MatchCondition, sampleClass: SampleClass,
            sampleStore: SampleStore, schema: DataSchema,
            attributes: AttributeSet, sampleFilter: SampleFilter): SS = {
    val samples = rawSamples(condition, sampleClass, sampleFilter,
        sampleStore, attributes)
    val unitHelper = new UnitsHelper(schema)
    apply(samples, condition, unitHelper, attributes)
  }

  def apply(samples: Iterable[Sample], condition: MatchCondition,
      unitsHelper: UnitsHelper, attributes: AttributeSet): SS
}

abstract class AbstractSampleSearch[ST <: SampleLike](condition: MatchCondition,
                                                      varianceSets: Map[String, VarianceSet], samples: Iterable[ST]) {

  protected def zTestSampleSize(s: ST): Int
  protected def sortObject(s: ST): (String, Int, Int)

  val MAX_PRINT = 20

  /**
   * Results of the search.
   */
  lazy val results: Iterable[ST] = {
    val searchResult = results(condition).toSeq.map(postProcessSample).sortBy(sortObject(_))

    val count = searchResult.size
    val countString = if (count > MAX_PRINT) s"(displaying $MAX_PRINT/$count)"
        else s"($count)"
    println(s"Search results $countString:")
    for (sample <- searchResult take MAX_PRINT) {
      println(sample)
    }

    searchResult
  }

  protected def postProcessSample(sample: ST): ST = sample

  protected def sampleAttributeValue(sample: ST, attribute: Attribute): Option[Double] =
    t.db.Sample.numericalValue(sample, attribute)

  private def paramComparison(sample: ST, attribute: Attribute,
                              controlGroupValue: ST => Option[Double],
                              comparator: (Double, Double) => Boolean): Option[Boolean] = {
    val testValue = sampleAttributeValue(sample, attribute)
    val reference = controlGroupValue(sample)
    (testValue, reference) match {
      case (Some(t), Some(r)) => Some(comparator(t, r))
      case _                  => None
    }
  }

  private def paramIsHigh(sample: ST, attribute: Attribute): Option[Boolean] = {
    paramComparison(sample, attribute,
      x => varianceSets.get(x.get(CoreParameter.SampleId)).
          flatMap(x => Option(x.upperBound(attribute, zTestSampleSize(sample)))),
      _ > _)
  }

  private def paramIsLow(sample: ST, attribute: Attribute): Option[Boolean] = {
    paramComparison(sample, attribute,
      x => varianceSets.get(x.get(CoreParameter.SampleId)).
          flatMap(x => Option(x.lowerBound(attribute, zTestSampleSize(sample)))),
      _ < _)
  }

  private def results(condition: MatchCondition): Set[ST] =
    condition match {
      //Note: could optimise and-evaluation by not evaluating unnecessary conditions
      case and: AndMatch =>
        and.subConditions.asScala.map(results _).reduce(_ intersect _)
      case or: OrMatch =>
        or.subConditions.asScala.map(results _).reduce(_ union _)
      case at: AtomicMatch =>
        results(at)
    }

  private def doubleOption(d: java.lang.Double): Option[Double] =
    if (d == null) None else Some(d)

  private def results(condition: AtomicMatch): Set[ST] =
    samples.filter(matches(_, condition.matchType, condition.parameter,
        doubleOption(condition.param1))).toSet

  private def matches(s: ST, mt: MatchType, attr: Attribute,
                      threshold: Option[Double]): Boolean =
    mt match {
      case MatchType.High => paramIsHigh(s, attr).getOrElse(false)
      case MatchType.Low  => paramIsLow(s, attr).getOrElse(false)
      case MatchType.NormalRange =>
        !sampleAttributeValue(s, attr).isEmpty &&
          paramIsHigh(s, attr) == Some(false) &&
          paramIsLow(s, attr) == Some(false)
      case _ =>
        sampleAttributeValue(s, attr) match {
          case Some(v) =>
            mt match {
              case MatchType.AboveLimit => v >= threshold.get
              case MatchType.BelowLimit => v <= threshold.get
              case _                    => throw new Exception("Unexpected match type")
            }
          case None => false
        }
    }

  protected def doseLevelMap: Map[String, Int] =
    Map() ++ OTGSchema.allDoses.zipWithIndex

  protected def exposureTimeMap: Map[String, Int] =
    Map() ++ OTGSchema.allTimes.zipWithIndex
}
