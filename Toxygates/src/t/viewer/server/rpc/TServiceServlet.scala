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

package t.viewer.server.rpc

import com.google.gwt.user.server.rpc.RemoteServiceServlet

import javax.servlet.ServletConfig
import javax.servlet.ServletException
import t.Context
import t.Factory
import t.common.shared.DataSchema
import t.viewer.server.Configuration

abstract class TServiceServlet extends RemoteServiceServlet {
  protected def context: Context
  protected def factory: Factory

  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    try {
      localInit(Configuration.fromServletConfig(config))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

  //This method should initialise Factory and Context.
  //Public for test purposes
  def localInit(config: Configuration): Unit = {}

  protected def makeFactory(): Factory

  protected def baseConfig = context.config

  protected def schema: DataSchema

  protected def appName: String

  override def doUnexpectedFailure(t: Throwable) {
    t.printStackTrace()
    super.doUnexpectedFailure(t)
  }
}

abstract class StatefulServlet[State] extends TServiceServlet {

  /**
   * Identified this servlet's state in the user session.
   */
  protected def stateKey: String

  /**
   * Creates a new, blank state object.
   */
  protected def newState: State

  protected def getState(): State = {
    val r = getThreadLocalRequest().getSession().getAttribute(stateKey).
      asInstanceOf[State]
    if (r == null) {
      val ss = newState
      setState(ss)
      ss
    } else {
      r
    }
  }

  protected def setState(m: State) =
    getThreadLocalRequest().getSession().setAttribute(stateKey, m)
}
