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
  protected def read(prefix: String = ".", name: String) = {
    val r = new BufferedReader(new FileReader(prefix + name))
    try {
      var data = Vector[Array[String]]()
      while (r.ready()) {
        val l = r.readLine
        val cs = l.split("\t")
        data :+= cs
      }

      if (data.isEmpty) {
        Vector()
      } else {
        Vector.tabulate(data(0).length, data.length)((col, row) =>
          data(row)(col))
      }
    } finally {
      r.close
    }
  }

  /**
   * A map indexed by column.
   */
  def readMap(prefix: String = ".", name: String): Map[String, Seq[String]] = {
    val cs = read(prefix, name)
    Map() ++ cs.map(col => col.head -> col.tail)
  }
}
