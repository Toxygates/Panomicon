package t

import t.db.kyotocabinet.KCSeriesDB
import t.db._
import t.sparql.SampleStore

class OTGMatrixContext(baseConfig: BaseConfig) extends MatrixContext {

  private val data = baseConfig.data
  private val maps = new TRefresher(baseConfig)

  def triplestoreConfig: TriplestoreConfig = baseConfig.triplestoreConfig

  def probeMap = maps.latest.unifiedProbes

  def enumMaps = maps.latest.enumMaps
  def sampleMap = maps.latest.sampleMap

  lazy val samples: SampleStore =
    new SampleStore(baseConfig)

  /**
   * Obtain a reader for the absolute value/normalized intensity
   *  database.
   */
  def absoluteDBReader: MatrixDBReader[PExprValue] =
    data.absoluteDBReader(this)

  /**
   * Obtain a reader for the folds database.
   */
  def foldsDBReader: MatrixDBReader[PExprValue] =
    data.foldsDBReader(this)

  def timeSeriesBuilder: OTGTimeSeriesBuilder.type = OTGTimeSeriesBuilder
  def doseSeriesBuilder: OTGDoseSeriesBuilder.type = OTGDoseSeriesBuilder

  def timeSeriesDBReader: SeriesDB[OTGSeries] =
    KCSeriesDB(data.timeSeriesDb, false, timeSeriesBuilder, true)(this)
  def doseSeriesDBReader: SeriesDB[OTGSeries] =
    KCSeriesDB(data.doseSeriesDb, false, doseSeriesBuilder, true)(this)

  var testRun: Boolean = false

  def close() {
    samples.close()
  }
}
