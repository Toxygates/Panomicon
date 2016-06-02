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
object TSVFile extends FileReadable[Seq[Seq[String]]] {
  def read(prefix: String = ".", name: String) = {
    val r = new BufferedReader(new FileReader(prefix + name))
    val l1 = r.readLine()
    val cs = l1.split("\t")

    val headers = cs.toVector
    var data = Vector[Array[String]]()

    var split = List
    while (r.ready()) {
      val l = r.readLine
      val cs = l.split("\t")
      data :+= cs
    }

    val result = Vector.tabulate(cs.length, data.length)((col, row) =>
      data(row)(col))
    headers +: result
  }

  /**
   * A map indexed by column.
   */
  def readMap(prefix: String = ".", name: String): Map[String, Seq[String]] = {
    val cs = read(prefix, name)
    var r = Map[String, Seq[String]]()
    for (c <- cs) {
      r += (c(0) -> c.drop(1))
    }
    r
  }
}
