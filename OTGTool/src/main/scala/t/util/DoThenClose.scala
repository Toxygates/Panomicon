package t.util

import java.io.Closeable

object DoThenClose {
  def doThenClose[A <: Closeable, T](closeable: A)(f: A => T): T = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }
}
