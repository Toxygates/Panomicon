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

package t

import scala.language.existentials
import t.sparql.Triplestore
import org.openrdf.repository.RepositoryConnection
import t.db.Series
import t.db.SeriesBuilder
import t.db.Metadata
import t.db.ParameterSet
import t.db.file.TSVMetadata
import t.db.kyotocabinet.KCSeriesDB
import t.db.kyotocabinet.KCIndexDB
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.db.kyotocabinet.KCMatrixDB
import t.db.MatrixDBReader
import t.db.PExprValue
import t.db.BasicExprValue
import t.db.MatrixContext
import t.db.ExprValue
import t.db.TransformingWrapper
import t.db.MatrixDB

//TODO should BaseConfig be invariant between applications?
trait BaseConfig {
  def triplestore: TriplestoreConfig
  def data: DataConfig

  //TODO Should this be in context?
  def seriesBuilder: SeriesBuilder[S] forSome { type S <: Series[S] }

  def sampleParameters: ParameterSet

  def appName: String
}

case class TriplestoreConfig(url: String, updateUrl: String,
  user: String, pass: String, repository: String) {
  lazy val triplestore: RepositoryConnection = {
    if (repository != null && repository != "") {
      println("RemoteRepository connect to " + url)
      Triplestore.connectRemoteRepository(this)
    } else {
      println("SPARQLRepository connect to " + this.url + " and " + this.updateUrl)
      Triplestore.connectSPARQLRepository(this.url, this.updateUrl, user, pass)
    }
  }

  def get = new t.sparql.SimpleTriplestore(triplestore, updateUrl == null)
}

object DataConfig {
  def apply(dir: String, matrixDbOptions: String): DataConfig = {
    if (dir.startsWith(KCChunkMatrixDB.CHUNK_PREFIX)) {
      new ChunkDataConfig(dir, matrixDbOptions)
    } else {
      new DataConfig(dir, matrixDbOptions)
    }
  }
}

class DataConfig(val dir: String, val matrixDbOptions: String) {
  protected def exprFile: String = "expr.kct" + matrixDbOptions
  protected def foldFile: String = "fold.kct" + matrixDbOptions

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

  def seriesDb: String = s"$dir/series.kct" + KCSeriesDB.options

  def sampleDb: String = s"$dir/sample_index.kct"
  def probeDb: String = s"$dir/probe_index.kct"
  def enumDb: String = s"$dir/enum_index.kct"

  def sampleIndex: String = sampleDb + KCIndexDB.options
  def probeIndex: String = probeDb + KCIndexDB.options
  def enumIndex: String = enumDb + KCIndexDB.options

  //TODO remove the fold wrap when possible
  def foldWrap(db: MatrixDBReader[PExprValue]): MatrixDBReader[PExprValue] =
    new TransformingWrapper(db) {
      def tfmValue(x: PExprValue) = x.copy(value = Math.pow(2, x.value))
    }

  def absoluteDBReader(implicit c: MatrixContext): MatrixDBReader[ExprValue] =
    KCMatrixDB.get(exprDb, false)
  def foldsDBReader(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    foldWrap(foldsDBReaderNowrap)
  def foldsDBReaderNowrap(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    KCMatrixDB.getExt(foldDb, false)

  def extWriter(file: String)(implicit c: MatrixContext): MatrixDB[PExprValue, PExprValue] =
    KCMatrixDB.getExt(file, true)
}

class HashDataConfig(dir: String, matrixDbOptions: String)
extends DataConfig(dir, matrixDbOptions) {
  override def exprFile = "expr.kch" + matrixDbOptions
  override def foldFile = "fold.kch" + matrixDbOptions
}

class ChunkDataConfig(dir: String, matrixDbOptions: String) extends
  DataConfig(KCChunkMatrixDB.removePrefix(dir), matrixDbOptions) {
  override def exprFile: String = "expr.kch" + matrixDbOptions
  override def foldFile: String = "fold.kch" + matrixDbOptions

  override def absoluteDBReader(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    KCChunkMatrixDB(exprDb, false)

  override def foldsDBReaderNowrap(implicit c: MatrixContext): MatrixDBReader[PExprValue] =
    KCChunkMatrixDB(foldDb, false)

  override def extWriter(file: String)(implicit c: MatrixContext): MatrixDB[PExprValue, PExprValue] =
    KCChunkMatrixDB.apply(file, true)
}
