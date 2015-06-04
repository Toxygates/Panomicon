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

package otgviewer.server.rpc

import t.viewer.server.rpc.TServiceServlet
import otg.Factory
import otg.Context
import t.viewer.server.Configuration
import otgviewer.shared.OTGSchema
import otg.OTGBConfig

trait OTGServiceServlet extends TServiceServlet {
  override protected def context: Context = _context
  override protected def factory: Factory = _factory
  
  protected var _context: Context = _ 
  protected var _factory: Factory = _

  override abstract def localInit(config: Configuration) {
    _factory = makeFactory()
    _context = _factory.context(config.tsConfig, config.dataConfig)
    super.localInit(config)
  }
  
  protected def makeFactory(): Factory = new Factory
  
  override protected def baseConfig: OTGBConfig = context.config
  
  protected val schema: OTGSchema = new OTGSchema()

}