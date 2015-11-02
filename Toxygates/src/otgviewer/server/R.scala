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

package otgviewer.server

import java.util.logging.Logger
import t.viewer.server.Configuration
import scala.sys.process._
import org.rosuda.REngine.Rserve.RserveException
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.REXP
import scala.collection.immutable.Queue

class R() {
  private val logger = Logger.getLogger("R")

  private var cmds = Queue[String]()

  private var conn: RConnection = null

  def addCommand(cmd: String) = {
    cmds = cmds.enqueue(cmd)
  }

  def exec(): Option[REXP] = {
    try {
      conn = new RConnection
      if (cmds.nonEmpty) Some(exec(cmds)) else None
    } catch {
      case e: Exception => logger.severe(e.getMessage); None
    } finally {
      if (conn != null) conn.close()
    }
  }

  private def exec(q: Queue[String]): REXP = {
    q.dequeue match {
      case (x, Queue()) => eval(x)
      case (x, xs)      => eval(x); exec(xs)
    }
  }

  private def eval(cmd: String) = {
    val r = conn.parseAndEval(s"try($cmd)")
    if (r.inherits("try-error")) {
      throw new RserveException(conn, r.asString())
    } else {
      r
    }
  }

}
