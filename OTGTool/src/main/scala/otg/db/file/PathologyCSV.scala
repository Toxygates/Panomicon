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

package otg.db.file

import scala.io.Source
import otg.Pathology
import scala.annotation.tailrec
import scala.io.Codec
import scala.Vector

case class PathologyItem(barcode: String,
  expId: String,
  groupId: String,
  individualId: String,
  compoundName: String,
  doseLevel: String,
  exposureTime: String,
  organ: String,
  detail: Pathology)

/**
 * Open TG-GATEs pathology data CSV file.
 * May be downloaded from
 * http://dbarchive.biosciencedbc.jp/en/open-tggates/download.html
 */
class PathologyCSV(file: String) {
  def lines = Source.fromFile(file)(Codec.UTF8).getLines.toSeq

  val items = lines.drop(1).map(l => {
    val s = mergeQuoted(l.split(",").toVector)
    val finding = s(8)
    val topography = s(9)
    val grade = s(10)
    val spontaneous = s(11).toBoolean
    val p = Pathology(Some(finding), Some(topography), Some(grade),
        spontaneous, s(0))

    new PathologyItem(s(0), s(1), s(2), s(3), s(4), s(5), s(6),
      s(7), p)
  })

  /**
   * Merge strings such as "Bile duct, interlobular" back together
   * since they will have been split
   */
  @tailrec
  private def mergeQuoted(spl: Vector[String],
      soFar: Vector[String] = Vector()): Vector[String] = {
    spl match {
      case a +: b +: rest =>
        if (a.startsWith("\"") && !a.endsWith("\"")) {
          mergeEndquote(a.drop(1) + ",", b +: rest, soFar)
        } else {
          mergeQuoted(b +: rest, soFar :+ a)
        }
      case Vector(a) => soFar :+ a
      case Vector() => soFar
    }
  }

  /**
   * Look for a matching end quote
   */
  @tailrec
  private def mergeEndquote(soFar: String, spl: Vector[String],
      outerAcc: Vector[String]): Vector[String] = {
    spl match {
      case a +: rest =>
        if (a.endsWith("\"")) {
          mergeQuoted(rest, outerAcc :+ (soFar + a.dropRight(1)))
        } else {
          mergeEndquote(s"$soFar,$a", rest, outerAcc)
        }
      case Vector() =>
        throw new Exception("Parse error: unterminated quote")
    }
  }
}
