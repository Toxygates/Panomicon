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

import org.eclipse.rdf4j.repository.RepositoryConnection
import t.model.sample.OTGAttributeSet
import t.sparql.Triplestore

import scala.language.existentials

case class BaseConfig(triplestoreConfig: TriplestoreConfig, data: DataConfig) {

  type DataSeries = OTGSeries

  /*
   * Note: this might be better in the context instead.
   */
  def timeSeriesBuilder = OTGTimeSeriesBuilder
  def doseSeriesBuilder = OTGDoseSeriesBuilder

  /**
   * Default attribute set for batches that have no specific attributes defined
   * @return
   */
  def attributes = OTGAttributeSet.getDefault
}

case class TriplestoreConfig(url: String, updateUrl: String,
                             user: String, pass: String, repository: String) {
  lazy val triplestore: RepositoryConnection = {
    println("SPARQLRepository connect to " + this.url + " and " + this.updateUrl)
    Triplestore.connectSPARQLRepository(this.url, this.updateUrl, user, pass)
  }

  def getTriplestore() = new t.sparql.SimpleTriplestore(triplestore, updateUrl == null)
}

