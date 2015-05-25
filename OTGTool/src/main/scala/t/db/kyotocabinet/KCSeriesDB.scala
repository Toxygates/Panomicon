package t.db.kyotocabinet

import java.nio.ByteBuffer
import scala.annotation.tailrec
import kyotocabinet.DB
import t.db.ProbeMap
import t.db.SeriesDB
import otg.Context
import t.db.BasicExprValue
import t.db.MatrixContext
import t.db.Series
import t.db.SeriesBuilder
import t.db.SeriesPoint
import t.global.KCDBRegistry
import t.platform.Probe


object KCSeriesDB {
  val c20g = 20l * 1204 * 1204 * 1024
  val c1g = 1l * 1204 * 1204 * 1024
  val c4g = 1l * 1204 * 1204 * 1024
  //TODO possibly re-tune this
  val options = s"#bnum=10000000#apow=1#pccap=$c1g"
  
  /**
   * Options: linear, no alignment, 10 million buckets (approx 10% of size), 5g memory mapped
   */  
  def apply[S <: Series[S]](file: String, writeMode: Boolean, builder: SeriesBuilder[S])
  (implicit context: MatrixContext): KCSeriesDB[S] = {
    val db = KCDBRegistry.get(file, writeMode)    
    db match {
      case Some(d) => new KCSeriesDB(file, d, builder)
      case None => throw new Exception("Unable to get DB")
    }
  }
}

/**
 * The database format used for "series" data,
 * e.g. dose series and time series.
 * Indexed by species/organ/repeat, probe, compound and time/dose.
 
 * Key size: 12 bytes, value size: 40-70 bytes (for Open TG-Gates)
 * Expected number of records: < 100 million
 * Expected DB size: about 3 G
 * 
 * 
 */
class KCSeriesDB[S <: Series[S]](file: String, db: DB,
    builder: SeriesBuilder[S])(implicit val context: MatrixContext) extends 
KyotoCabinetDB(file, db) with SeriesDB[S] {
  
  private[this] def formKey(series: S): Array[Byte] = {
    val r = ByteBuffer.allocate(12)
    r.putInt(series.probe)
    r.putLong(series.classCode)    
    r.array()
  }

  private[this] def extractKey(data: Array[Byte]): S = {
    val b = ByteBuffer.wrap(data)
    val probe = b.getInt
    val sclass = b.getLong    
    builder.build(sclass, probe)    
  }

  private[this] def formValue(series: S): Array[Byte] = {
    val r = ByteBuffer.allocate(14 * series.points.size)
    for (v <- series.points) {
      r.putInt(v.code)
      r.putDouble(v.value.value)
      r.putChar(v.value.call)
    }
    r.array()
  }

  private[this] def extractValue(data: Array[Byte], into: S): S = {
    val b = ByteBuffer.wrap(data)
    val pmap = context.probeMap
    val vs = Vector() ++ (0 until data.size / 14).map(i => {
      val p = b.getInt
      val v = b.getDouble
      val c = b.getChar
      SeriesPoint(p, BasicExprValue(v, c, pmap.unpack(into.probe)))
    })
    builder.rebuild(into, vs)    
  }

   /**
   * Obtain the series that match the constraints specified in the key.
   */
  def read(key: S): Iterable[S] = {
    val keys = builder.keysFor(key).map(formKey)
    val data = db.get_bulk(keys.toArray, false)
    var r = Vector[S]()
    for (i <- Range(0, data.length, 2)) {
      val k = data(i)
      val v = data(i + 1)
      r :+= extractValue(v, extractKey(k))
    }
    r
  }
  
  def addPoints(s: S): Unit = {
    get(formKey(s)) match {
      case Some(d) =>
        val exist = extractValue(d, s)
        write(exist.addPoints(s, builder))
      case None =>
        write(s)
    }
  }
  
  def removePoints(s: S): Unit = {
    val k = formKey(s)
    get(k) match {
      case Some(d) =>
        val exist = extractValue(d, s)
        val removed = exist.removePoints(s, builder)
        if (removed.points.size > 0) {
          write(removed)
        } else {
          db.remove(k)
        }
      case None =>
        write(s)
    }
  }
  
   /**
   * Insert or replace a series.
   */
  private def write(s: S): Unit = {
    val key = formKey(s)
    val v = formValue(s)
    db.set(key, v)
  }
}