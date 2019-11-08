package t.util

import java.io.{Closeable => JCloseable}
import t.Closeable

object DoThenClose {
  def doThenClose[A <: JCloseable, T](closeable: A)(f: A => T): T = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }

  //note: this name was chosen so as to not clash with the method
  //above after type erasure.
  def useThenClose[A <: Closeable, T](closeable: A)(f: A => T): T = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }
}
