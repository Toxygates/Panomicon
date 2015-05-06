package t.db.kyotocabinet

import kyotocabinet.DB
import t.global.KCDBRegistry

abstract class KyotoCabinetDB(file: String, db: DB) {
  def get(key: Array[Byte]): Option[Array[Byte]] = {
    val d = db.get(key)
    if (d != null) {
      Some(d)
    } else {
      None
    }
  }
  
  def release() {
    KCDBRegistry.release(file)
  }
}