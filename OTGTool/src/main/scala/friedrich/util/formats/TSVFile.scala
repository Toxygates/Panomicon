/**
 * Part of the Friedrich bioinformatics framework.
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */
package friedrich.util.formats
import java.io._

import scala.io.{Codec, Source}

/**
 * Reads files with tab-separated columns.
 * Result stored in column-major format.
 */
object TSVFile extends FileReadable[Seq[Seq[String]]] {
  import t.util.DoThenClose._

  protected def read(prefix: String = ".", name: String) = {
    doThenClose(Source.fromFile(prefix + name)(Codec.UTF8))(r => {
      var data = Vector[Array[String]]()
      for {l <- r.getLines} {
        val cs = l.split("\t")
        if (cs.nonEmpty && l.length > 0) {
          //ignore empty lines
          data :+= cs
        }
      }

      if (data.isEmpty) {
        Vector()
      } else {
        Vector.tabulate(data(0).length, data.length)((col, row) =>
          if (data(row).length > col) {
            data(row)(col)
          } else {
            ""
          })
      }
    })
  }

  /**
   * A map indexed by column.
   */
  def readMap(prefix: String = ".", name: String): Map[String, Seq[String]] = {
    val cs = read(prefix, name)
    Map() ++ cs.map(col => col.head -> col.tail)
  }
}
