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

/**
 * A series of expression values ranging over some independent variable
 */
abstract class Series[This <: Series[This]](val probe: Int, val points: Seq[SeriesPoint]) {
  this: This =>
  def classCode(implicit mc: MatrixContext): Long

  def addPoints(from: This, builder: SeriesBuilder[This]): This = {
    val keep = points.filter(x => !from.points.exists(y => y.code == x.code))
    builder.rebuild(this, keep ++ from.points)
  }

  def removePoints(in: This, builder: SeriesBuilder[This]): This = {
    val keep = points.filter(x => !in.points.exists(y => y.code == x.code))
    builder.rebuild(this, keep)
  }

  def values = points.map(_.value)

  def presentValues = points.map(_.value).filter(_.call != 'A').map(_.value)

  def probeStr(implicit mc: MatrixContext) = mc.probeMap.unpack(probe)

  /**
   * A modified (less constrained) version of this series that
   * represents the constraints for ranking with a single probe.
   */
  def asSingleProbeKey: This

  def constraints: Map[String, String] = Map()
}

/**
 * Code is the encoded enum value of the independent variable, e.g.
 * 24hr for a time series.
 */
case class SeriesPoint(code: Int, value: ExprValue)

/**
 * An object that can decode/encode the database format of series
 * for a particular application.
 */
trait SeriesBuilder[S <: Series[S]] {
  /**
   * Construct a series with no points.
   */
  def build(sampleClass: Long, probe: Int)(implicit mc: MatrixContext): S

  /**
   * Insert points into a series.
   */
  def rebuild(from: S, points: Iterable[SeriesPoint]): S

  /**
   * Generate all keys belonging to the (partially specified)
   * series key.
   */
  def keysFor(group: S)(implicit mc: MatrixContext): Iterable[S]

  /**
   * Using values from the given MatrixDB, construct all possible series for the
   * samples indicated in the metadata.
   */
  def makeNew[E >: Null <: ExprValue](from: MatrixDBReader[E],
      md: Metadata, samples: Iterable[Sample])(implicit mc: MatrixContext): Iterable[S]

  /**
   * Using values from the given MatrixDB, construct all possible series for the
   * samples indicated in the metadata.
   */
  def makeNew[E >: Null <: ExprValue](from: MatrixDBReader[E], md: Metadata)
  (implicit mc: MatrixContext): Iterable[S] = makeNew(from, md, md.samples)

  /**
   * Enum keys that are necessary for this SeriesBuilder.
   */
  def enums: Iterable[String]

  def standardEnumValues: Iterable[(String, String)]

  /**
   * Expected time points for the given series
   */
  def expectedTimes(key: S): Seq[String]

  protected def packWithLimit(enum: String, field: String, mask: Int)(implicit mc: MatrixContext) = {
    val p = mc.enumMaps(enum)(field)
    if (p > mask) {
      throw new Exception(s"Unable to pack Series: $field in '$enum' is too big ($p)")
    }
    (p & mask).toLong
  }

  def presentMeanByProbe(ds: Iterable[ExprValue]): Iterable[ExprValue] = {
    val byProbe = ds.groupBy(_.probe)
    byProbe.map(x => ExprValue.presentMean(x._2, x._1))
  }

  /**
   * Sort time points and insert missing ones (as 0.0/A) according to standard expectations.
   */
  def normalize(data: Iterable[S])(implicit mc: MatrixContext): Iterable[S]
}
