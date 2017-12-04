/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
import scala.collection.{ Map => CMap }
import t.db._

class ParseException(msg: String) extends Exception

/**
 * Raw data in CSV files.
 * Will be read one sample at a time.
 * Call files may be absent (by passing in an empty list), in which case
 * all values are treated as present.
 */
class CSVRawExpressionData(exprFiles: Iterable[String],
    callFiles: Iterable[String], expectedSamples: Option[Int],
    parseWarningHandler: (String) => Unit) extends RawExpressionData {

  private def samplesInFile(file: String) = {
    val line = Source.fromFile(file).getLines.next
    val columns = line.split(",", -1).map(_.trim)
    columns.drop(1).toVector.map(s => Sample(unquote(s)))
  }

  override lazy val samples: Iterable[Sample] =
    exprFiles.toVector.flatMap(f => samplesInFile(f)).distinct

  override lazy val probes: Iterable[String] =
    exprFiles.toVector.flatMap(f => probesInFile(f)).distinct

  private def probesInFile(file: String) = {
    val lines = Source.fromFile(file).getLines
    lines.next
    for (line <- lines;
      columns = line.split(",", -1).map(_.trim);
      probe = unquote(columns.head)
    ) yield probe
  }

  private val expectedColumns = expectedSamples.map(_ + 1)

  private[this] def traverseFile[T](file: String,
    lineHandler: (Array[String], String) => Unit): Array[String] = {
    println("Read " + file)
    val s = Source.fromFile(file)
    val ls = s.getLines
    val columns = ls.next.split(",", -1).map(_.trim)

    //Would this be too strict?
//    if (expectedColumns != None && expectedColumns.get != columns.size) {
//      throw new ParseException(
//          s"Wrong number of column headers - expected $expectedColumns got ${columns.size}")
//    }
//
    var call: Vector[T] = Vector.empty
    for (l <- ls) {
      lineHandler(columns, l)
    }
    s.close
    columns
  }

  private[this] def readValuesFromTable[T](file: String, ss: Set[Sample],
    extract: String => T): CMap[Sample, CMap[String, T]] = {

    var raw: Vector[Array[String]] = Vector.empty
    val columns = traverseFile(file, (columns, l) => {
      val spl = l.split(",", -1).map(_.trim)
      if (expectedColumns != None && spl.size < expectedColumns.get) {
        val wmsg =
            s"Too few columns on line (expected $expectedColumns, got ${spl.size}. Line starts with: " + l.take(30)
        parseWarningHandler(wmsg)
      } else {
        raw :+= spl
      }
    })

    var r = Map[Sample, CMap[String, T]]()
    for (c <- 1 until columns.size;
      sampleId = unquote(columns(c));
      sample = Sample(sampleId);
      if ss.contains(sample)) {

      var col = scala.collection.mutable.Map[String, T]()
      col ++= raw.map(r => {
        val pr = unquote(r(0))
        try {
          pr -> extract(r(c))
        } catch {
          case nfe: NumberFormatException =>
            val wmsg = "Number format error: unable to parse string '" + r(c) + "' for probe " +
              pr + " and sample " + sampleId
            parseWarningHandler(wmsg)
            throw nfe
        }
      })
      r += sample -> col
    }

    r
  }

  private[this] def unquote(x: String) = x.replace("\"", "")

  private[this] def readCalls(file: String, ss: Set[Sample]): CMap[Sample, CMap[String, Char]] = {
    //take the second char in a string like "A" or "P"
    readValuesFromTable(file, ss, x => x(1))
  }

  /**
   * Read expression values from a file.
   * The result is a map that maps samples to probe IDs and values.
   */
  private[this] def readExprValues(file: String, ss: Set[Sample]): CMap[Sample, CMap[String, Double]] = {
    import java.lang.{Double => JDouble}
    readValuesFromTable(file, ss, _.toDouble).mapValues(_.filter(v => !JDouble.isNaN(v._2)))
  }

  override def data(ss: Iterable[Sample]): CMap[Sample, CMap[String, FoldPExpr]] = {
    val expr = exprFiles.map(readExprValues(_, ss.toSet))
    val call = callFiles.map(readCalls(_, ss.toSet))

    //NB, samples must not be repeated across files - we should check this
    val allExprs = Map() ++ expr.flatten
    val allCalls = Map() ++ call.flatten

    allExprs.map {
      case (s, col) => {
        val sampleCalls = allCalls.get(s)
        s -> col.map {
          case (p, v) => {
            if (sampleCalls != None && !sampleCalls.get.contains(p)) {
              throw new Exception(s"No call available for probe $p in sample $s")
            }
            val useCall = sampleCalls.map(_(p)).getOrElse('P')
            (p -> (v, useCall, Double.NaN))
          }
        }
      }
    }
  }

  def data(s: Sample): CMap[String, FoldPExpr] = {
    data(Set(s)).head._2
  }
}
