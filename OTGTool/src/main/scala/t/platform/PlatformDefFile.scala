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

package t.platform

import scala.io.Source
import t.db.kyotocabinet.KCIndexDB

//TODO extract standard columns and move outside affy package
import t.platform.affy.{GOMF, GOBP, GOCC, Entrez}

object ProbeRecord {
  /**
   * Generate RDF predicates for each probe.
   * Note that we must use double quotes for the value:
   * Affymetrix definitions contain strings with single quotes.
   */

  def asRdfTerms(key: String, value: String): Seq[(String, String)] = {
    key match {
      //example value: 0050839

      case "go" | GOMF.key | GOBP.key | GOCC.key =>
        Seq((s"t:$key", s"<http://purl.obolibrary.org/obo/GO_$value>"))
      case Entrez.key =>
        Seq((s"t:$key", "\"" + value + "\""),
            ("<http://bio2rdf.org/kegg_vocabulary:x-ncbigene>",
                s"<http://bio2rdf.org/ncbigene:$value>"))
      case _ =>
        Seq((s"t:$key", "\"" + value + "\""))
    }
  }
}

case class ProbeRecord(id: String, annotations: Map[String, Iterable[String]]) {
  def annotationRDF: String =
    (for (
      (k, vs) <- annotations;
      v <- vs;
      (predicate, objct) <- ProbeRecord.asRdfTerms(k, v);
      item = s"$predicate $objct"
    ) yield item).mkString("; ")
}

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
          println(s"tprobe:${r.id} ${r.annotationRDF}.")
        }
    }
  }
}
