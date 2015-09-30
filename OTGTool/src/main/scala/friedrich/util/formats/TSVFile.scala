/**
 * Part of the Friedrich bioinformatics framework.
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */
package friedrich.util.formats
import java.io._

/**
 * Reads files with tab-separated columns.
 * Result stored in column-major format.
 */
object TSVFile extends FileReadable[Array[Array[String]]] {
  def read(prefix: String = ".", name: String) = {
    val r = new BufferedReader(new FileReader(prefix + name))
    val l1 = r.readLine()
    val cs = l1.split("\t")

    //obtained the number of columns
    val result = new Array[Vector[String]](cs.size)
    for (i <- 0 until cs.size) {
      result(i) = Vector(cs(i))
    }

    while (r.ready()) {
      val l = r.readLine
      val cs = l.split("\t")
      var i = 0
      while (i < cs.size) {
        result(i) :+= cs(i)
        i += 1
      }
    }
    result.map(_.toArray)
  }

  def readMap(prefix: String = ".", name: String) = {
    val cs = read(prefix, name)
    var r = Map[String, Array[String]]()
    for (c <- cs) {
      r += (c(0) -> c.drop(1))
    }
    r
  }
}
