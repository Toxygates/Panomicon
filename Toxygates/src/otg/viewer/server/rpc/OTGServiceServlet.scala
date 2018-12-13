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

package otg.viewer.server.rpc

import otg._
import otg.viewer.shared.OTGSchema
import t.viewer.server.Configuration

import t.viewer.server.rpc.TServiceServlet

trait OTGServiceServlet extends TServiceServlet {
  override protected def context: OTGContext = _context
  override protected def factory: OTGFactory = _factory

  protected var _context: OTGContext = _
  protected var _factory: OTGFactory = _

  override abstract def localInit(config: Configuration) {
    _factory = makeFactory()
    _context = _factory.context(config.tsConfig, config.dataConfig(_factory))
    super.localInit(config)
  }

  protected def makeFactory() = new OTGFactory

  override protected def baseConfig: OTGBConfig = context.config

  protected val schema: OTGSchema = new OTGSchema()

  def appName = "Toxygates"
}
