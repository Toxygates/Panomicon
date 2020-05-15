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

package otg.viewer.server.rpc

import otg.viewer.shared.{Series => SSeries}
import t.{OTGDoseSeriesBuilder, OTGSeries, OTGSeriesBuilder, OTGTimeSeriesBuilder}
import t.db.SeriesDB
import t.common.shared.SeriesType
import t.viewer.server.rpc.OTGServiceServlet

class SeriesServiceImpl extends
t.viewer.server.rpc.SeriesServiceImpl[OTGSeries] with OTGServiceServlet {

  implicit def mat = context.matrix
  implicit def ctxt = context

  protected def ranking(db: SeriesDB[OTGSeries], key: OTGSeries) =
    new t.SeriesRanking(db, key)

  override protected def asShared(s: OTGSeries, geneSym: String): SSeries =
    Conversions.asJava(s, geneSym)

  override protected def fromShared(s: SSeries): OTGSeries =
    Conversions.asScala(s)

  override protected def getDB(seriesType: SeriesType): SeriesDB[OTGSeries] = {
    import SeriesType._
    seriesType match {
      case Time => mat.timeSeriesDBReader
      case Dose => mat.doseSeriesDBReader
    }
  }

  protected def builder(s: SeriesType): OTGSeriesBuilder = {
    import SeriesType._
    s match {
      case Dose => OTGDoseSeriesBuilder
      case Time => OTGTimeSeriesBuilder
    }
  }

  def expectedIndependentPoints(stype: SeriesType, s: SSeries): Array[String] =
    builder(stype).expectedIndependentVariablePoints(fromShared(s)).toArray

}
