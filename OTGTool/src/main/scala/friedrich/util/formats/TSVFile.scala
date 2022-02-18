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
object TSVFile {
  import t.util.DoThenClose._

  protected def read(name: String, unquote: Boolean = false) = {
    doThenClose(Source.fromFile(name)(Codec.UTF8))(r => {
      var data = Vector[Array[String]]()
      for { l <- r.getLines.filter(!isHeaderLine(_)) } {
        val cs = l.split("\t").map(normalize(unquote, _))
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

  def isHeaderLine(line: String): Boolean = {
    line.startsWith("#")
  }

  def normalize(unquote: Boolean, value: String) =
    if (unquote)
      value.replace("\"", "")
    else
      value

  /**
   * A map indexed by column.
   */
  def readMap(name: String, unquote: Boolean): Map[String, Seq[String]] = {
    val cs = read(name, unquote)
    Map() ++ cs.map(col => normalize(unquote, col.head) -> col.tail)
  }

  def readHeaderMap(name: String): Map[String, Seq[String]] = {
    ???
  }
}
