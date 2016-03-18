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

package t.platform.affy
import scala.io._
import scala.Vector

/**
 * A tool for converting Affymetrix annotation files to the T
 * platform definition format.
 */
object AffymetrixConverter {

  val columns = List(GOBP, GOCC, GOMF,
      Swissprot, RefseqTranscript, ProbeID, GeneChip, Title,
      Entrez, Species, Unigene, Symbol)

  val columnLookup = Map() ++ columns.map(c => c.title -> c)

  val listColumns = columns.filter(_.isList)
  val ignorePrefix = "AFFX"

  def main(args: Array[String]): Unit = {
    if (args.size < 1) {
      throw new Exception("Please specify the input file name (.csv)")
    }
    val input = args(0)
    val output = args(0).replace(".csv", "_platform.tsv")
    convert(input, output)
  }

  def convert(input: String, output: String): Unit = {
    val data =
      Source.fromFile(input).getLines.toVector.dropWhile(_.startsWith("#")).
        map(l => rejoin(l.split(",").toList))
    val columns = data(0).map(procItem)
    val pdata = data.drop(1).map(_.map(procItem))
    val idColumnOffset = columns.indexOf(ProbeID.title)
    if (idColumnOffset == -1) {
      throw new Exception(s"ID column '${ProbeID.title}' not found in data")
    }

    println(s"Writing output to $output")
    val ps = new java.io.PrintStream(new java.io.FileOutputStream(output))
    Console.withOut(ps) {
      for (pline <- pdata) {
        procLine(pline, columns, idColumnOffset)
      }
    }
  }

  def procLine(l: Seq[String], columns: Seq[String], idColumn: Int) {
    val id = l(idColumn)
    if (id.startsWith(ignorePrefix)) {
      return //don't print this probe
    }
    var out = Vector[String]()
    var annotations = Vector[String]()
    for (
      i <- 0 until l.size; if columns.size > i; c = columnLookup.get(columns(i));
      field = l(i)
    ) {
      if (idColumn == i) {
        out :+= s"$field"
      }
      c match {
        case None =>
        case Some(c) =>
          if (c.annotKey != None) {
            c.annotations(field).foreach { annotations :+= _ }
          }
      }
    }

    out :+= annotations.mkString(",")
    println(out.mkString("\t"))
  }

  // This is to re-join strings like "asdf, asdf","234, 234" which would incorrectly
  // have been split into 4 (should be 2)
  def rejoin(ss: List[String]): List[String] = {
    ss match {
      case Nil      => Nil
      case s :: Nil => List(s)
      //This case will match as many times as necessary to reconstruct the full string
      case s :: t :: ts => if (s.startsWith("\"") &&
        !s.endsWith("\"")) {
        rejoin((s + "," + t) :: ts)
      } else {
        s :: rejoin(t :: ts)
      }
    }
  }

  def procItem(x: String): String = {
    val n = if (x.startsWith("\"")) {
      x.substring(1)
    } else {
      x
    }

    val n2 = if (n.endsWith("\"")) {
      n.substring(0, n.size - 1)
    } else {
      n
    }
    n2.trim
  }
}
