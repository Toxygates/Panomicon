package otgviewer.server

import otgviewer.shared.DataFilter
import otg.Filter
import otg.sparql.RDFConnector
import javax.servlet.http.HttpSession

object UtilsS {
  def useConnector[C <: RDFConnector, T](conn: C, f: C => T): T = {
    try {
      conn.connect()
      f(conn)
    } finally {
      conn.close()
    }
  }

  def useConnector[C <: RDFConnector, T](conn: C, f: C => T, onFailure: T): T =
    gracefully(() => {
      conn.connect()
      f(conn)
    },
      onFailure,
      () => conn.close())
  
  def nullToNone[T <: AnyRef](x: T): Option[T] = {
    if (x == null) {
      None
    } else {
      Some(x)
    }
  }
  
  def gracefully[T](f: () => T, onFailure: T, finalizer: () => Unit): T = {
    try {
      f()
    } catch {
      case e => {
        e.printStackTrace()
        onFailure
      }
    } finally {
      finalizer()
    }
  }
  
}