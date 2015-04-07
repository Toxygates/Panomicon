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
t.common.server.rpc.SeriesServiceImpl[OTGSeries] with OTGServiceServlet {

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
   
  def expectedTimes(s: SSeries): Array[String] = {
    val key = fromShared(s)
    println("Key: " + key)
    context.matrix.seriesBuilder.expectedTimes(key).toArray
  }
}