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
      cmds.nonEmpty match {
        case true => Some(exec(cmds))
        case _    => None
      }
    } catch {
      case e: Exception => logger.severe(e.getMessage); None
    } finally {
      if (conn != null) conn.close()
    }
  }
  
  private def exec(q: Queue[String]): REXP = {
    q.dequeue match {
      case (x, Queue()) => eval(x)
      case (x, xs) => eval(x); exec(xs)
    }
  }

  private def eval(cmd: String) = {
    val r = conn.parseAndEval(s"try($cmd)")
    r.inherits("try-error") match {
      case true => throw new RserveException(conn, r.asString())
      case _    => r
    }
  }
 
}
