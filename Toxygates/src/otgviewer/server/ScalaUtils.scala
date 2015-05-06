package otgviewer.server

import t.sparql.Triplestore

object ScalaUtils {
  def gracefully[T](f: () => T, onFailure: T): T = {
    try {
      f()
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