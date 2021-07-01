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
 * Raw data in CSV files.
 * Will be read a few samples at a time, saving memory, possibly sacrificing speed.
 * Call files may be absent, in which case all values are treated as present call (P).
 */
class CSVRawExpressionData(exprFile: String,
    callFile: Option[String], expectedSamples: Option[Int],
    parseWarningHandler: (String) => Unit) extends ColumnExpressionData {

  import t.util.DoThenClose._

  private def samplesInFile(file: String) = {
    val firstLine = doThenClose(Source.fromFile(file))(_.getLines.next)
    val columns = firstLine.split(",", -1).map(_.trim)
    columns.drop(1).toArray.map(s => Sample(unquote(s)))
  }

  override lazy val samples: Array[Sample] =
    samplesInFile(exprFile).distinct.toArray

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

  protected val expectedColumns = expectedSamples.map(_ + 1)

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

  protected def probeBuffer[T] = {
    val r = ArrayBuffer[T]()
    r.sizeHint(probes.size)
    r
  }

  protected def sampleBuffer[T] = {
    val r = ArrayBuffer[T]()
    r.sizeHint(samples.size)
    r
  }

  protected def readValuesFromTable[T : ClassTag](file: String, ss: Iterable[Sample],
                                                  extract: String => T): CMap[Sample, Array[T]] = {
    val samples = ss.map(_.sampleId).toSet

    val raw = probeBuffer[IndexedSeq[String]]
    var keptColumns: Option[IndexedSeq[String]] = None
    var keptIndices: Option[IndexedSeq[Int]] = None

    traverseFile(file, (columns, l) => {
      if(keptColumns == None) {
        keptColumns = Some(ArrayBuffer(columns.head) ++
           columns.filter(x => samples.contains(x)))
        keptIndices = Some(ArrayBuffer(0) ++
           columns.indices.filter(i => samples.contains(columns(i))))
      }

      val spl = l.split(",", -1).map(x => x.trim)
      if (expectedColumns != None && spl.size < expectedColumns.get) {
        val wmsg =
            s"Too few columns on line (expected $expectedColumns, got ${spl.size}. Line starts with: " + l.take(30)
        parseWarningHandler(wmsg)
      } else {
        raw += keptIndices.get.map(spl)
      }
    })

    val rawAndProbes = raw zip probes

    var r = mutable.Map[Sample, Array[T]]()
    for (c <- 1 until keptColumns.get.size;
      sampleId = keptColumns.get(c); sample = Sample(sampleId)) {

      val col = rawAndProbes.map {case (row, probe) =>
        try {
          extract(row(c))
        } catch {
          case nfe: NumberFormatException =>
            val wmsg = s"Number format error: unable to parse string '${row(c)}' for probe $probe" +
              s" and sample $sampleId"
            parseWarningHandler(wmsg)
            throw nfe
        }
      }
      r += sample -> col.toArray
    }

    r
  }

  private[this] def unquote(x: String) = x.replace("\"", "")

  protected def readCalls(file: String, ss: Iterable[Sample]): CMap[Sample, Array[Char]] = {
    //get the second char in a string like "A" or "P"
    readValuesFromTable(file, ss, x => unquote(x)(0))
  }

  lazy val defaultCalls = probes.map(_ => 'P')

  /**
   * Read expression values from a file.
   * The result is a map that maps samples to probe IDs and values.
   */
  protected def readExprValues(file: String, ss: Iterable[Sample]): CMap[Sample, Array[Double]] = {
    readValuesFromTable(file, ss, _.toDouble)
  }

  import java.lang.{Double => JDouble}
  override def data(ss: Iterable[Sample]): CMap[Sample, CMap[ProbeId, FoldPExpr]] = {
    val exprs = readExprValues(exprFile, ss.toSeq.distinct)
    val calls = callFile.map(readCalls(_, ss.toSeq.distinct)).getOrElse(Map())

    exprs.map { case (s, col) => {
        val sampleCalls = calls.getOrElse(s, defaultCalls)
        s -> (Map() ++ (probes.iterator zip (col.iterator zip sampleCalls.iterator)).map {
          case (p, (v, c)) => {
            (p -> (v, c, Double.NaN))
          }
        }).filter(v => ! JDouble.isNaN(v._2._1))
      }
    }
  }

  def data(s: Sample): CMap[ProbeId, FoldPExpr] = {
    data(Set(s)).headOption.map(_._2).getOrElse(Map())
  }
}

/**
 * Immediately caches all samples in memory to gain more speed.
 */
class CachedCSVRawExpressionData(exprFile: String,
    callFile: Option[String], expectedSamples: Option[Int],
    parseWarningHandler: (String) => Unit)
    extends CSVRawExpressionData(exprFile, callFile, expectedSamples, parseWarningHandler) {

  var exprCache: CMap[Sample, Array[Double]] = Map()
  var callsCache: CMap[Sample, Array[Char]] = Map()

  exprCache = readExprValues(exprFile, samples)
  callsCache = callFile match {
    case Some(f) => readCalls(f, samples)
    case _ => Map()
  }

  /*
   * Read all data at once, ignoring the ss parameter.
   */
   override protected def readValuesFromTable[T : ClassTag](file: String, ss: Iterable[Sample],
    extract: String => T): CMap[Sample, Array[T]] = {

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
      r += (sample -> raw.map(_(c)).toArray)
    }
    r
  }

  override def calls(x: Sample): Array[Option[Char]] =
    callsCache.getOrElse(x, defaultCalls).map(Some(_))

  override def exprs(x: Sample): Array[Option[Double]] =
    exprCache(x).map(Some(_))

  override protected def readCalls(file: String, ss: Iterable[Sample]): CMap[Sample, Array[Char]] = {
    val (preExisting, notYetRead) = ss.partition(callsCache.contains(_))
    if (notYetRead.nonEmpty) {
      callsCache ++= super.readCalls(file, notYetRead)
    }
    val sampleSet = ss.toSet
    callsCache.filter(x => sampleSet.contains(x._1))
  }

  override protected def readExprValues(file: String, ss: Iterable[Sample]): CMap[Sample, Array[Double]] = {
    val (preExisting, notYetRead) = ss.partition(exprCache.contains(_))
    if (notYetRead.nonEmpty) {
      exprCache ++= super.readExprValues(file, notYetRead)
    }
    val sampleSet = ss.toSet
    exprCache.filter(x => sampleSet.contains(x._1))
  }

}
