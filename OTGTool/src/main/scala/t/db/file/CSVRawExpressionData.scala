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

package t.db.file

import scala.io.Source
import scala.collection.{mutable, Map => CMap}
import t.db._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class ParseException(msg: String) extends Exception

/**
 * Raw data from CSV files, cached in memory.
 * Call files may be absent, in which case all values are treated as present call (P).
 */
class CSVRawExpressionData(exprFile: String,
    callFile: Option[String], expectedSamples: Option[Int],
    parseWarningHandler: (String) => Unit) extends ColumnExpressionData {

  import t.util.DoThenClose._

  //Consistency check
  for {callFileR <- callFile} {
    val notInCallsSamples = samplesInFile(callFileR).toSet -- samplesInFile(exprFile)
    if (notInCallsSamples.nonEmpty) {
      throw new Exception(s"Invalid CSVRawExpressionData - the following samples have no P/A calls available: ${notInCallsSamples mkString ","}")
    }
  }

  protected val expectedColumns = expectedSamples.map(_ + 1)

  lazy val defaultCalls = probes.map(_ => 'P')

  lazy val exprCache: CMap[Sample, Array[Double]] = readValuesFromTable(exprFile, _.toDouble)
  lazy val callsCache: CMap[Sample, Array[Char]] = callFile match {
    case Some(f) => readValuesFromTable(f, x => unquote(x)(0))
    case _ => Map()
  }

  private def samplesInFile(file: String) = {
    val firstLine = doThenClose(Source.fromFile(file))(_.getLines.next)
    val columns = firstLine.split(",", -1).map(_.trim)
    columns.drop(1).map(s => Sample(unquote(s)))
  }

  override lazy val samples: Array[Sample] =
    samplesInFile(exprFile).distinct

  override lazy val probes: Array[String] =
    probesInFile(exprFile).toArray

  private def probesInFile(file: String): Seq[ProbeId] = {
    doThenClose(Source.fromFile(file))(ls => {
      (for {
        line <- ls.getLines().drop(1)
        columns = line.split(",", -1).map(_.trim)
        probe = unquote(columns.head)
      } yield probe).toList //force evaluation
    })
  }

  protected def traverseFile[T](file: String,
    lineHandler: (Array[String], String) => Unit): Array[String] = {
    println("Read " + file)
    doThenClose(Source.fromFile(file))(s => {
      val ls = s.getLines
      val columns = ls.next.split(",", -1).map(x => unquote(x.trim))

      for (l <- ls) {
        lineHandler(columns, l)
      }
      columns
    })
  }

  protected def probeBuffer[T]: ArrayBuffer[T] = {
    val r = ArrayBuffer[T]()
    r.sizeHint(probes.size)
    r
  }

  protected def sampleBuffer[T]: ArrayBuffer[T] = {
    val r = ArrayBuffer[T]()
    r.sizeHint(samples.size)
    r
  }

  private[this] def unquote(x: String) = x.replace("\"", "")

  import java.lang.{Double => JDouble}
  override def data(ss: List[Sample]): CMap[Sample, CMap[ProbeId, FoldPExpr]] = {
    val exprs = filterExprValues(ss.distinct)
    val calls = filterCalls(ss.distinct)

    exprs.map { case (s, col) => {
        val sampleCalls = calls.getOrElse(s, defaultCalls)
        s -> (mutable.Map() ++ (probes.iterator zip (col.iterator zip sampleCalls.iterator)).map {
          case (p, (v, c)) => {
            (p -> (v, c, Double.NaN))
          }
        }).filter(v => ! JDouble.isNaN(v._2._1))
      }
    }
  }

  def data(s: Sample): CMap[ProbeId, FoldPExpr] = {
    data(List(s)).headOption.map(_._2).getOrElse(Map())
  }

  override def calls(x: Sample): Array[Option[Char]] =
    callsCache.getOrElse(x, defaultCalls).map(Some(_))

  override def exprs(x: Sample): Array[Option[Double]] =
    exprCache(x).map(Some(_))

  protected def filterCalls(ss: Iterable[Sample]): CMap[Sample, Array[Char]] = {
    val sampleSet = ss.toSet
    callsCache.filter(x => sampleSet.contains(x._1))
  }

  protected def filterExprValues(ss: Iterable[Sample]): CMap[Sample, Array[Double]] = {
    val sampleSet = ss.toSet
    exprCache.filter(x => sampleSet.contains(x._1))
  }

  /*
   * Read the entire table, converting it into type T.
   */
  protected def readValuesFromTable[T: ClassTag](file: String, extract: String => T): CMap[Sample, Array[T]] = {

    val raw = probeBuffer[IndexedSeq[T]]

    traverseFile(file, (columns, l) => {

      val spl = l.split(",", -1).map(x => x.trim)
      if (expectedColumns != None && spl.size < expectedColumns.get) {
        val wmsg =
          s"Too few columns on line (expected $expectedColumns, got ${spl.size}. Line starts with: " + l.take(30)
        parseWarningHandler(wmsg)
      } else {
        try {
          raw += spl.drop(1).map(extract)
        } catch {
          case nfe: NumberFormatException =>
            val wmsg = s"Number format error: unable to parse row for probe ${spl(0)}. " +
              "Try with non-cached mode for more detailed error message."
            parseWarningHandler(wmsg)
            throw nfe
        }
      }
    })

    var r = mutable.Map[Sample, Array[T]]()

    for (c <- 0 until samples.size;
         sample = samples(c)) {
      r += (sample -> raw.map(_ (c)).toArray)
    }
    r
  }
}
