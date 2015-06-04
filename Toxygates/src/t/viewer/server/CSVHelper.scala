/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

import t.db.ExprValue


object CSVHelper {

  /**
   * This trait can be implemented by classes that wish to write data as
   * CSV files.
   */
  trait CSVFile {
    def write(out: String) {
      println("Rows " + rows + " cols " + columns)

      val outb = new java.io.BufferedWriter(new java.io.FileWriter(out))
      var x = 0
      var y = 0
      while (x < columns && y < rows) {

        outb.write(format(apply(x, y)))
        if (x < columns - 1) {
          outb.write(",")
        }
        x += 1
        if (x == columns) {
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

    def columns: Int
    def rows: Int

    /**
     * Obtain the item at the specified coordinate.
     */
    def apply(x: Int, y: Int): Any

  }

  /**
   * Write expression values to a CSV files.
   * The given probes and geneIds only will be written.
   * The generated url will be returned.
   */
  def writeCSV(dir: String, urlbase: String, 
      probes: Seq[String], titles: Seq[String], 
      geneIds: Seq[String], expr: Seq[Seq[ExprValue]]): String = {

    if (expr.size == 0) {
      throw new Exception("No data supplied")
    }
    
    //TODO pass the file prefix in from outside
    val file = "otg" + System.currentTimeMillis + ".csv"
    val fullName = dir + "/" + file
    
    new CSVFile {
    	def columns = titles.size + 2
    	def rows = expr.size + 1
      def apply(x: Int, y: Int) = if (y == 0) {
        if (x > 1) {
          titles(x - 2)
        } else if (x == 1) {
          "Gene"
        } else {
          "Probe"
        }
      } else {  //y < 0
        if (x == 0) { 
          probes(y - 1)
        } else if (x == 1) {
          geneIds(y - 1)
        } else {
          expr(y - 1)(x - 2).value
        }
      }
    }.write(fullName)

    urlbase + "/" + file
  }
}