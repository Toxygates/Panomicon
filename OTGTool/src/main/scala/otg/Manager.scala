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

package otg

import t.BaseConfig
import otg.platform.SSOrthTTL
import friedrich.util.CmdLineOptions
import t.DataConfig
import t.TriplestoreConfig

object Manager extends t.Manager[Context, OTGBConfig] with CmdLineOptions {
  override protected def handleArgs(args: Array[String])(implicit context: Context) {

    args(0) match {
      case "orthologs" =>
        val output = require(stringOption(args, "-output"),
          "Please specify an output file with -output")
        val inputs = require(stringListOption(args, "-inputs"),
          "Please specify input files with -inputs")

        new SSOrthTTL(context.probes, inputs, output).generate
      case _ => super.handleArgs(args)
    }
  }

  lazy val factory: Factory = new otg.Factory()

  def initContext(bc: OTGBConfig): Context = otg.Context(bc)

  def makeBaseConfig(ts: TriplestoreConfig, d: DataConfig): OTGBConfig =
    OTGBConfig(ts, d)
}

case class OTGBConfig(triplestore: TriplestoreConfig, data: DataConfig) extends BaseConfig {
  def seriesBuilder = OTGSeries

  def sampleParameters = otg.db.OTGParameterSet

  def appName = "Toxygates"
}
