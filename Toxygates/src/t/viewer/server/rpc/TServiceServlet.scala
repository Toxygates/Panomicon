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

package t.viewer.server.rpc

import com.google.gwt.user.server.rpc.RemoteServiceServlet

import javax.servlet.ServletConfig
import javax.servlet.ServletException
import t.BaseConfig
import t.Context
import t.Factory
import t.viewer.server.Configuration
import t.common.shared.DataSchema

abstract class TServiceServlet extends RemoteServiceServlet {
  protected def context: Context
  protected def factory: Factory

  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }

  //This method should initialise Factory and Context.
  //Public for test purposes
  def localInit(config: Configuration): Unit = {}

  protected def makeFactory(): Factory

  protected def baseConfig = context.config

  protected def schema: DataSchema
}
