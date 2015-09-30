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

package t.platform

import scala.io.Source
import t.db.kyotocabinet.KCIndexDB

object ProbeRecord {
  //Note that we must use double quote for the value.
  //Affymetrix definitions contain strings with single quotes.
  def asRdfTerm(key: String, value: String) = {
    key match {
      case "go" | "gomf" | "gobp" | "gocc" => s"<http://bio2rdf.org/go:$value>"
      case _                               => "\"" + value + "\""
    }
  }
}

case class ProbeRecord(id: String, annotations: Map[String, Iterable[String]])

class PlatformDefFile(file: String) {

  val records: Iterable[ProbeRecord] =
    Source.fromFile(file).getLines.toVector.flatMap(parseProbe(_))

  def parseProbe(data: String): Option[ProbeRecord] = {
    val s = data.split("\t")
    if (s.length >= 2) {
      val annots = s(1).split(",").toVector
      val pas = annots.map(a => a.split("=")).filter(_.size == 2).map(a => (a(0), a(1)))
      val grouped = Map() ++ pas.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
      Some(ProbeRecord(s(0), grouped))
    } else if (s.length == 1) {
      Some(ProbeRecord(s(0), Map()))
    } else {
      println(s"Unable to parse line: $data")
      None
    }
  }
}

object PlatformDefFile {
  def main(args: Array[String]) {
    val idxDb = if (args.size > 1) Some(KCIndexDB(args(1), true)) else None

    idxDb match {
      case Some(db) =>
        try {
          for (r <- new PlatformDefFile(args(0)).records) {
            db.get(r.id) match {
              case Some(i) =>
              case None =>
                db.put(r.id)
                println("Insert " + r.id)
            }
          }
        } finally {
          db.release
        }
      case None =>
        for (r <- new PlatformDefFile(args(0)).records) {
          println(s"${r.id} ${r.annotations("refseq")}")
        }
    }
  }
}
