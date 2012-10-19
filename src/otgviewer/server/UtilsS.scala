package otgviewer.server

import otgviewer.shared.DataFilter
import otg.Filter
import otg.RDFConnector
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
  
  def nullToNone[T <: AnyRef](x: T): Option[T] = {
    if (x == null) {
      None
    } else {
      Some(x)
    }
  }
}