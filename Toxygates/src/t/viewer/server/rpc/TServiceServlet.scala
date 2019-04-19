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

  /**
   * Perform any necessary initialisation of this servlet
   * once the Configuration object is available.
   */
  def localInit(config: Configuration): Unit = {}

  protected def baseConfig = context.config

  protected def schema: DataSchema

  protected def appName: String

  override def doUnexpectedFailure(t: Throwable) {
    t.printStackTrace()
    super.doUnexpectedFailure(t)
  }

  /**
   * Obtain an in-session object that can be used for synchronizing session state between servlet threads.
   * Code that expects to read from another thread or publish to another thread via the session
   * should lock on this. (This could happen, for example, if the session manages transactional state.)
   * The servlet container will ensure that different requests from the same client see an up-to-date
   * session object, so this mutex is not needed for that purpose.
   *
   * This is not currently used.
   *
   * Inspired by: https://stackoverflow.com/questions/616601/is-httpsession-thread-safe-are-set-get-attribute-thread-safe-operations
   * And http://web.archive.org/web/20110806042745/http://www.ibm.com/developerworks/java/library/j-jtp09238/index.html
   *
   */
  protected def mutex: AnyRef = {
    val mutId = "mutex"
    val ses = getThreadLocalRequest.getSession
    Option(ses.getAttribute(mutId)) match {
      case Some(m) => m
      case None =>
        ServletSessions.synchronized {
          if (ses.getAttribute(mutId) == null) {
            ses.setAttribute(mutId, new Object)
          }
          ses.getAttribute(mutId)
        }
    }
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

object ServletSessions

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
