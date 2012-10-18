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

  def attribOrElse[T](session: HttpSession, name: String, alternative: T): T = {
    val v = session.getAttribute(name)
    if (v != null) { v.asInstanceOf[T] } else { alternative }
  }
}