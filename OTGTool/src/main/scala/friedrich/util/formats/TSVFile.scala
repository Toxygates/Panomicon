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
 *
 * Columns IDs are identified by the first line.
 * After this line, "header" lines starting with # may occur, containing additional metadata.
 * Other lines are data lines.
 */
object TSVFile {
  import t.util.DoThenClose._

  protected def read(name: String, unquote: Boolean,
                     lineFilter: String => Boolean) = {
    doThenClose(Source.fromFile(name)(Codec.UTF8))(r => {
      var data = Vector[Array[String]]()

      var first = true
      for { l <- r.getLines } {
        val cs = l.split("\t").map(normalize(unquote, _))
        //ignore empty lines.
        //Filter all lines except the first (column headers) through the line filter.
        if (cs.nonEmpty && l.length > 0 && (first || lineFilter(l))) {
          first = false
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

  def isHeaderLine(line: String): Boolean =
    line.startsWith("#")

  def unHeaderize(data: String): String =
    data.replaceFirst("#", "")

  def normalize(unquote: Boolean, value: String) =
    if (unquote)
      value.replace("\"", "")
    else
      value

  /**
   * A map of data lines indexed by column. Columns are identified by the first non-header line.
   */
  def readMap(name: String, unquote: Boolean): Map[String, Seq[String]] = {
    val cs = read(name, unquote, !isHeaderLine(_))
    Map.empty ++ cs.map(col => normalize(unquote, col.head) -> col.tail)
  }

  /**
   * A map of header lines (also tab-separated) indexed by column.
   */
  def readHeaderMap(name: String, unquote: Boolean): Map[String, Seq[String]] = {
    val cs = read(name, unquote, isHeaderLine(_))
    Map.empty ++ cs.map(col => normalize(unquote, col.head) -> col.tail.map(unHeaderize(_)))
  }
}
