/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otg

import friedrich.util.CmdLineOptions
import otg.platform.SSOrthTTL
import t.platform.Species._
import t._

object Manager extends t.Manager[OTGContext, OTGBConfig] with CmdLineOptions {
  override protected def handleArgs(args: Array[String])(implicit context: OTGContext) {

    args(0) match {
      case "orthologs" =>
        val output = require(stringOption(args, "-output"),
          "Please specify an output file with -output")
        val intermineURL = require(stringOption(args, "-intermineURL"),
          "Please specify an intermine URL with -intermineURL, e.g. https://mizuguchilab.org/targetmine/service")
        val intermineAppName = require(stringOption(args, "-intermineAppName"),
          "Please specify an intermine app name with -intermineAppName, e.g. targetmine")

        val spPairs = Seq((Rat, Human), (Human, Mouse), (Mouse, Rat))

        val conn = new t.intermine.Connector(intermineAppName, intermineURL)
        new SSOrthTTL(context.probes, output).generateFromIntermine(conn, spPairs)
      case _ => super.handleArgs(args)
    }
  }

  lazy val factory: OTGFactory = new otg.OTGFactory()

  def initContext(bc: OTGBConfig): OTGContext = otg.OTGContext(bc)

  def makeBaseConfig(ts: TriplestoreConfig, d: DataConfig): OTGBConfig =
    OTGBConfig(ts, d)
}

case class OTGBConfig(triplestore: TriplestoreConfig, data: DataConfig) extends BaseConfig {
  type DataSeries = OTGSeries

  def timeSeriesBuilder = OTGTimeSeriesBuilder
  def doseSeriesBuilder = OTGDoseSeriesBuilder

//  def sampleParameters = otg.db.OTGParameterSet

  def appName = "Toxygates"

  def attributes = otg.model.sample.AttributeSet.getDefault
}
