package otgviewer.server

import otgviewer.shared.DataFilter
import otg.Filter
import javax.servlet.http.HttpSession
import t.sparql.Triplestore

object UtilsS {
  def useTriplestore[C <: Triplestore, T](conn: C, f: C => T, onFailure: T): T = {
    try {
      f(conn)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        onFailure
    }
  }
      
  def nullToNone[T <: AnyRef](x: T): Option[T] = {
    if (x == null) {
      None
    } else {
      Some(x)
    }
  }
  
  def tryOrFailWith[T](f: () => T, onFailure: T, finalizer: () => Unit): T = {
    try {
      f()
    } catch {
      case e: Exception =>
        e.printStackTrace()
        onFailure
    } finally {
      finalizer()
    }
  }
}