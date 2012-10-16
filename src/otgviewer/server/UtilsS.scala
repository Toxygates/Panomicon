package otgviewer.server

import otgviewer.shared.DataFilter
import otg.Filter
import otg.RDFConnector

object UtilsS {
  def useConnector[C <: RDFConnector, T](conn: C, f: C => T): T = {
    try {
      conn.connect()
      f(conn)
    } finally {
      conn.close()
    }
  }
}