package t.viewer.server.servlet

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

import javax.servlet.{ServletConfig, ServletException}
import javax.servlet.http.HttpServlet
import t.{BaseConfig, Context, Factory}
import t.viewer.server.Configuration
import t.viewer.shared.OTGSchema

/**
 * Minimal trait for HTTPServlets to participate in the framework with a basic configuration.
 */
trait MinimalTServlet {
  this: HttpServlet =>

  protected def context: Context = _context
  protected def factory: Factory = _factory

  protected var _context: Context = _
  protected var _factory: Factory = _

  //Subclasses should override init() and call this method
  @throws(classOf[ServletException])
  def tServletInit(config: ServletConfig): Configuration = {
    try {
      val conf = Configuration.fromServletConfig(config)
      _factory = new Factory
      _context = _factory.context(conf.tsConfig, conf.dataConfig(_factory))
      conf
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

  protected def baseConfig: BaseConfig = context.config

  protected val schema = new OTGSchema()
}
