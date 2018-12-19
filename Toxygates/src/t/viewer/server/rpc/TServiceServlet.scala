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

object ServletSessionMutex

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

  /**
   * Obtain an in-session object that can be used for synchronizing session state between servlet threads.
   * Code that expects to read from another thread or publish to another thread via the session
   * should lock on this.
   *
   * Inspired by: https://stackoverflow.com/questions/616601/is-httpsession-thread-safe-are-set-get-attribute-thread-safe-operations
   * And http://web.archive.org/web/20110806042745/http://www.ibm.com/developerworks/java/library/j-jtp09238/index.html
   *
   */
  protected def mutex: AnyRef = {
    val mutId = "mutex"
    val ses = getThreadLocalRequest().getSession()
    Option(ses.getAttribute(mutId)) match {
      case Some(m) => m
      case None =>
        ServletSessionMutex.synchronized {
          ses.setAttribute(mutId, new Object)
          ses.getAttribute(mutId)
        }
    }
  }

  /**
   * Set state that may be shared between two servlet threads.
   */
  def setSharedSessionState[T](key: String, t: T) = mutex.synchronized {
    getThreadLocalRequest.getSession.setAttribute(key, t)
  }

  /**
   * Get state that may be shared between two servlet threads.
   */
  def getSharedSessionState(key: String) = mutex.synchronized {
    getThreadLocalRequest.getSession.getAttribute(key)
  }

  /**
   * Attempts to get another service's state, but does not initialise it if
   * it is missing.
   */
  protected def getOtherServiceState[OState](key: String): Option[OState] =
    mutex.synchronized {
     Option(getThreadLocalRequest.getSession.getAttribute(key).
      asInstanceOf[OState])
    }

}
