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

package t.server.viewer.rpc

import com.google.gwt.user.server.rpc.RemoteServiceServlet
import javax.servlet.{ServletConfig}
import t.server.viewer.Configuration
import t.server.viewer.servlet.MinimalTServlet

/**
 * A MinimalTServlet that is also a GWT RemoteServiceServlet
 */
abstract class TServiceServlet extends RemoteServiceServlet with MinimalTServlet {

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    val conf = tServletInit(config)
    localInit(conf)
  }

  /**
   * Perform any necessary initialisation of this servlet
   * once the Configuration object is available.
   */
  def localInit(config: Configuration): Unit = {}

  override def doUnexpectedFailure(t: Throwable) {
    t.printStackTrace()
    super.doUnexpectedFailure(t)
  }

  protected def hasSession: Boolean = (getThreadLocalRequest.getSession(false) != null)

  /**
   * Set session state.
   */
  def setSessionAttr[T](key: String, t: T) = {
    getThreadLocalRequest.getSession.setAttribute(key, t)
  }

  /**
   * Get session state.
   */
  def getSessionAttr[T >: Null](key: String): T = {
    if (hasSession) {
        getThreadLocalRequest.getSession.getAttribute(key).asInstanceOf[T]
    } else {
      null
    }
  }

  /**
   * Attempts to get another service's state, but does not initialise it if
   * it is missing.
   */
  protected def getOtherServiceState[OState >: Null](key: String): Option[OState] =
    Option(getSessionAttr[OState](key))

}

abstract class StatefulServlet[State >: Null] extends TServiceServlet {

  /**
   * Identified this servlet's state in the user session.
   */
  protected def stateKey: String

  /**
   * Creates a new, blank state object.
   */
  protected def newState: State

  /**
   * Get this service's main session state object, or initialise
   * it if it is missing.
   */
  protected def getState(): State = {
    val r = getSessionAttr[State](stateKey)
    if (r == null) {
      val ss = newState
      setState(ss)
      ss
    } else {
      r
    }
  }

  protected def setState(m: State) =
    getThreadLocalRequest.getSession.setAttribute(stateKey, m)
}
