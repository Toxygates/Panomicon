/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.server

import java.util.Calendar

object CSVHelper {

  /**
   * This trait can be implemented by classes that wish to write data as
   * CSV files.
   */
  trait CSVFile {
    def write(out: String) {
      println("Rows " + rowCount + " cols " + colCount)

      val outb = new java.io.BufferedWriter(new java.io.FileWriter(out))
      var x = 0
      var y = 0
      while (x < colCount && y < rowCount) {

        outb.write(format(apply(x, y)))
        if (x < colCount - 1) {
          outb.write(",")
        }
        x += 1
        if (x == colCount) {
          x = 0
          y += 1
          outb.write("\n")
        }
      }
      outb.close()
    }

    def format(obj: Any): String = {
      obj match {
        case Some(x) => {
          x match {
            case d: Double => format(d)
            case _ => ""
          }
        }
        case None => ""
        case d: Double => d.toString
        case i: Int    => i.toString
        case _         => "\"" + obj.toString + "\""
      }
    }

    def colCount: Int
    def rowCount: Int

    /**
     * Obtain the item at the specified coordinate.
     */
    def apply(x: Int, y: Int): Any

  }

  /**
   * Write expression values to a CSV files.
   * The given probes and geneIds only will be written.
   * The generated url will be returned.
   *
   * @param textCols Extra columns to be inserted to the left
   * @param expr Row-major data
   */
  def writeCSV(namePrefix: String, dir: String,
      textCols: Seq[(String, Seq[String])],
      rowTitles: Seq[String], colTitles: Seq[String],
      expr: Seq[Seq[Any]]): String = {

    if (expr.size == 0) {
      throw new Exception("No data supplied")
    }

    val name = filename(namePrefix, "csv")
    val fullName = dir + "/" + name

    new CSVFile {
    	def colCount = textCols.size + colTitles.size + 1
    	def rowCount = expr.size + 1
      def apply(x: Int, y: Int) = if (y == 0) {
        if (x == 0) {
          ""
        } else if (x < textCols.size + 1) {
          textCols(x - 1)._1
        } else {
          colTitles(x - textCols.size - 1)
        }
      } else {  //y > 0
        if (x == 0) {
          rowTitles(y - 1)
        } else if (x < textCols.size + 1) {
        	textCols(x - 1)._2(y - 1)
        } else {
          expr(y - 1)(x - textCols.size - 1)
        }
      }
    }.write(fullName)

    name
  }

  def writeCSV(namePrefix: String, dir: String, csvFile: CSVFile): String = {
    val name = filename(namePrefix, "csv")
    val fullName = dir + "/" + name
    csvFile.write(fullName)
    name
  }

  def writeCSV(namePrefix: String, dir: String,
    rowTitles: Seq[String], colTitles: Seq[String],
    data: Seq[Seq[Any]]): String =
    writeCSV(namePrefix, dir, Seq(), rowTitles, colTitles,
      data)

  def quasiRandomPrefix(namePrefix: String): String = {
    val cal = Calendar.getInstance
    val dfmt = s"${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    s"$namePrefix-$dfmt-${System.currentTimeMillis() % 10000}"
  }

  /**
   * Generate a new quasi-random but meaningful filename
   */
  def filename(namePrefix: String, suffix: String): String = {
    s"${quasiRandomPrefix(namePrefix)}.$suffix"
  }
}
