package t

import t.db.kyotocabinet.{KCIndexDB, KCSeriesDB}
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.db._

object DataConfig {
  def apply(dir: String, matrixDbOptions: String): DataConfig = {
    //Task: remove this logic when the kcchunk: prefix has been removed from all config files
    val dataDir = if (dir.startsWith(KCChunkMatrixDB.CHUNK_PREFIX)) {
      KCChunkMatrixDB.removePrefix(dir)
    } else dir
    new DataConfig(dataDir, matrixDbOptions)
  }
}

class DataConfig(val dir: String, val matrixDbOptions: String) {
  protected def exprFile: String = "expr.kch" + matrixDbOptions

  protected def foldFile: String = "fold.kch" + matrixDbOptions

  /**
   * Is the data store in maintenance mode? If it is, updates are not allowed.
   */
  def isMaintenanceMode: Boolean = {
    val f = new java.io.File(s"$dir/MAINTENANCE_MODE")
    if (f.exists()) {
      println(s"$f exists")
      true
    } else {
      false
    }
  }

  def exprDb: String = s"$dir/$exprFile"

  def foldDb: String = s"$dir/$foldFile"

  def timeSeriesDb: String = s"$dir/time_series.kct" + KCSeriesDB.options

  def doseSeriesDb: String = s"$dir/dose_series.kct" + KCSeriesDB.options

  def sampleDb: String = s"$dir/sample_index.kct"

  def probeDb: String = s"$dir/probe_index.kct"

  def enumDb: String = s"$dir/enum_index.kct"

  def sampleIndex: String = sampleDb + KCIndexDB.options

  def probeIndex: String = probeDb + KCIndexDB.options

  def enumIndex: String = enumDb + KCIndexDB.options

  def mirnaDir = s"$dir/mirna"

  def absoluteDBReader(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    MatrixDB.get(exprDb, false)

  def foldsDBReader(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    MatrixDB.get(foldDb, false)

  def extWriter(file: String)(implicit c: MatrixContext): MatrixDB[PExprValue, PExprValue] =
    MatrixDB.get(file, true)
}
