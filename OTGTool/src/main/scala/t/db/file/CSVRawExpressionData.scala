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

package t.db.file

import scala.io.Source
import t.db.RawExpressionData
import t.db.Sample
import scala.collection.{ Map => CMap }

class CSVRawExpressionData(exprFiles: Iterable[String],
    callFiles: Option[Iterable[String]]) extends RawExpressionData {

  private[this] def traverseFile[T](file: String,
    lineHandler: (Array[String], String) => Unit): Array[String] = {
    println("Read " + file)
    val s = Source.fromFile(file)
    val ls = s.getLines
    val columns = ls.next.split(",", -1).map(_.trim)
    var call: Vector[T] = Vector.empty
    for (l <- ls) {
      lineHandler(columns, l)
    }
    s.close
    columns
  }

  private[this] def readValuesFromTable[T](file: String,
    extract: String => T): CMap[Sample, CMap[String, T]] = {

    var raw: Vector[Array[String]] = Vector.empty
    val columns = traverseFile(file, (columns, l) => {
      raw :+= l.split(",", -1).map(_.trim)
    })

    var data = scala.collection.mutable.Map[Sample, CMap[String, T]]()

    for (c <- 1 until columns.size) {
      val barcode = Sample(unquote(columns(c)))
      var col = scala.collection.mutable.Map[String, T]()

      col ++= raw.map(r => unquote(r(0)) -> extract(r(c)))
      data += (barcode -> col)
    }
    data
  }

  private[this] def unquote(x: String) = x.replace("\"", "")

  private[this] def readCalls(file: String): CMap[Sample, CMap[String, Char]] = {
    //take the second char in a string like "A" or "P"
    readValuesFromTable(file, x => x(1))
  }

  /**
   * Read expression values from a file.
   * The result is a map that maps samples to probe IDs and values.
   */
  private[this] def readExprValues(file: String): CMap[Sample, CMap[String, Double]] = {
    readValuesFromTable(file, _.toDouble)
  }

  import scala.collection.mutable.{Map => MuMap}  //more efficient to build
  lazy val data: CMap[Sample, CMap[String, (Double, Char, Double)]] = {
    val expr = Map() ++ exprFiles.map(readExprValues(_)).flatten

    val call = callFiles.map(fs => Map() ++ fs.map(readCalls(_)).flatten)

    var r = MuMap[Sample, CMap[String, (Double, Char, Double)]]()

    for ((s, pv) <- expr) {
      for (calls <- call; if !calls.contains(s)) {
        throw new Exception(s"No calls available for sample $s")
      }

      val cm = call.map(_(s))

      var out = MuMap[String, (Double, Char, Double)]()
      for ((p, v) <- pv) {
        if (cm != None && !cm.get.contains(p)) {
          throw new Exception(s"No call available for probe $p in sample $s")
        }
        val usec = cm.map(_(p)).getOrElse('P')

        //TODO p-value handling
        out += (p -> (v, usec, Double.NaN))
      }
      r += (s -> out)
      println(s"Finished reading data for sample $s")
    }
    r
  }
}
