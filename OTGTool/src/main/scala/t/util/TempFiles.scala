package t.util

import java.io.File

/**
 * A way of managing a set of temporary files that can be 
 * released collectively
 */
class TempFiles {
  var registry: Vector[File] = Vector()

  def makeNew(prefix: String, suffix: String): File = {
    val f = File.createTempFile(prefix, suffix)
    println("Created temporary file " + f)
    registry :+= f
    f
  }

  def dropAll() {
    for (f <- registry) {
      println("Deleting temporary file " + f)
      f.delete()
    }
  }
}