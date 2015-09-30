/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otgviewer.server.rpc

import t.db.kyotocabinet.KCSeriesDB
import otgviewer.shared.DBUnavailableException
import t.global.KCDBRegistry
import otg.OTGSeries
import t.db.SeriesDB
import t.SeriesRanking
import otgviewer.shared.{Series => SSeries}
import t.common.shared.SampleClass

class SeriesServiceImpl extends 
t.viewer.server.rpc.SeriesServiceImpl[OTGSeries] with OTGServiceServlet {

  implicit def mat = context.matrix
  implicit def ctxt = context
  
  override protected def ranking(db: SeriesDB[OTGSeries], key: OTGSeries) = 
    new otg.SeriesRanking(db, key)
  
  override protected def asShared(s: OTGSeries): SSeries = 
    Conversions.asJava(s)
    
  override protected def fromShared(s: SSeries): OTGSeries =
    Conversions.asScala(s)
  
  override protected def getDB(): SeriesDB[OTGSeries] = {
    //TODO organise this better
    try {
      val file = baseConfig.data.seriesDb
      val db = KCDBRegistry.get(file, false)
      db match {
        case Some(d) => new KCSeriesDB(file, d, OTGSeries) {
          override def read(key: OTGSeries): Iterable[OTGSeries] = {
            OTGSeries.normalize(super.read(key))
          }
        }
        case None => throw new Exception("Unable to get DB")
      }
    } catch {
      case e: Exception => throw new DBUnavailableException()
    }
  }
   
  //TODO lift up this method
  def expectedTimes(s: SSeries): Array[String] = {
    val key = fromShared(s)
    println("Key: " + key)
    context.matrix.seriesBuilder.expectedTimes(key).toArray
  }
}