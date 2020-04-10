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

package t

import scala.language.existentials

import org.eclipse.rdf4j.repository.RepositoryConnection

import t.db._
import t.db.kyotocabinet._
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.model.sample.AttributeSet
import t.sparql.Triplestore
import scala.reflect._

trait BaseConfig {
  def triplestore: TriplestoreConfig
  def data: DataConfig

  type DataSeries <: Series[DataSeries]

  /*
   * Note: this might be better in the context instead.
   */
  def timeSeriesBuilder: SeriesBuilder[DataSeries]
  def doseSeriesBuilder: SeriesBuilder[DataSeries]

  def attributes: AttributeSet

  def appName: String
}

case class TriplestoreConfig(url: String, updateUrl: String,
  user: String, pass: String, repository: String) {
  lazy val triplestore: RepositoryConnection = {
    println("SPARQLRepository connect to " + this.url + " and " + this.updateUrl)
    Triplestore.connectSPARQLRepository(this.url, this.updateUrl, user, pass)
  }

  def get = new t.sparql.SimpleTriplestore(triplestore, updateUrl == null)
}

object DataConfig {
  import KCChunkMatrixDB._
  def apply(dir: String, matrixDbOptions: String): DataConfig = {
    if (dir.startsWith(CHUNK_PREFIX)) {
      new DataConfig(removePrefix(dir), matrixDbOptions)
    } else {
      throw new Exception("Unexpected data dir type")
    }
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

  //Task: remove the fold wrap when possible
  def foldWrap(db: MatrixDBReader[PExprValue]): MatrixDBReader[PExprValue] =
    new TransformingWrapper(db) {
      def tfmValue(x: PExprValue) = {
        val r = x.copy(value = Math.pow(2, x.value))
        r.isPadding = x.isPadding
        r
      }
    }

  def absoluteDBReader(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    MatrixDB.get(exprDb, false)
  def foldsDBReader(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    foldWrap(foldsDBReaderNowrap)
  def foldsDBReaderNowrap(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    MatrixDB.get(foldDb, false)

  def extWriter(file: String)(implicit c: MatrixContext): MatrixDB[PExprValue, PExprValue] =
    MatrixDB.get(file, true)
}
