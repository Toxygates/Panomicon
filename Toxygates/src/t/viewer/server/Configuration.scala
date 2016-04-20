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

package t.viewer.server

import javax.servlet.ServletConfig
import otg.OTGContext
import t.TriplestoreConfig
import t.DataConfig
import t.BaseConfig
import t.db.MatrixContext
import t.Factory
import t.Context
import t.sparql.Instances

object Configuration {
  /**
   * Create a new Configuration from the ServletConfig.
   */
  def fromServletConfig(config: ServletConfig) = {
    val servletContext = config.getServletContext()

    def p(x: String) = servletContext.getInitParameter(x)

    /**
     * These parameters are read from <context-param> tags in WEB-INF/web.xml.
     */
    new Configuration(p("repositoryName"),
      p("dataDir"),
      p("csvDir"),
      p("csvUrlBase"),
      p("repositoryURL"),
      p("updateURL"),
      p("repositoryUser"),
      p("repositoryPassword"),
      p("instanceName"),
      p("webappHomeDir"),
      p("matrixDbOptions"),
      p("feedbackReceivers"),
      p("feedbackFromAddress"),
      p("targetmineApiKey"))
  }
}

/**
 * A bridge from ServletConfig to Context.
 */
class Configuration(val repositoryName: String,
    val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String,
    val repositoryUrl: String = null,
    val updateUrl: String = null,
    val repositoryUser: String = null,
    val repositoryPass: String = null,
    val instanceName: String = null,
    val webappHomeDir: String = null,
    val matrixDbOptions: String = null,
    val feedbackReceivers: String = null,
    val feedbackFromAddress: String = null,
    val targetmineApiKey: String = null) {

  def this(owlimRepository: String, toxygatesHome: String, foldsDBVersion: Int) =
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"),
      System.getProperty("otg.csvUrlBase"))

  def tsConfig = TriplestoreConfig(repositoryUrl, updateUrl,
    repositoryUser, repositoryPass, repositoryName)

  def dataConfig(f: Factory) = f.dataConfig(toxygatesHomeDir, matrixDbOptions)

  def context(f: Factory): Context = f.context(tsConfig, dataConfig(f))

  def instanceURI: Option[String] =
    if (instanceName == null || instanceName == "") {
      None
    } else {
      Some(Instances.defaultPrefix + "/" + instanceName)
    }

}
