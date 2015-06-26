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

//TODO should BaseConfig be invariant between applications?
trait BaseConfig {
  def triplestore: TriplestoreConfig
  def data: DataConfig

  //TODO Should this be in context?
  def seriesBuilder: SeriesBuilder[S] forSome { type S <: Series[S] }

  def sampleParameters: ParameterSet
}

case class TriplestoreConfig(url: String, updateUrl: String,
  user: String, pass: String, repository: String) {
  lazy val triplestore: RepositoryConnection = {
    if (repository != null && repository != "") {
      println("RemoteRepository connect to " + url)
      Triplestore.connectRemoteRepository(this)
    } else {
      println("SPARQLRepository connect to " + this.url + " and " + this.updateUrl)
      Triplestore.connectSPARQLRepository(this.url, this.updateUrl)
    }
  }
}

case class DataConfig(dir: String, matrixDbOptions: String) {
  def exprDb: String = s"$dir/expr.kct" + matrixDbOptions
  def foldDb: String = s"$dir/fold.kct" + matrixDbOptions
  def seriesDb: String = s"$dir/series.kct" + KCSeriesDB.options

  def sampleIndex: String = s"$dir/sample_index.kct" + KCIndexDB.options
  def probeIndex: String = s"$dir/probe_index.kct" + KCIndexDB.options
  def enumIndex: String = s"$dir/enum_index.kct" + KCIndexDB.options
}
