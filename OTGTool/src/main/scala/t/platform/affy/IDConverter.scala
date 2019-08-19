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

package t.platform.affy

/**
 * An ID conversion table that converts non-affy gene and probe IDs,
 * e.g. ensembl, into affy IDs, based on a set of affymetrix annotations.
 */
class IDConverter(affyFile: String, column: AffyColumn) {

  val data = Converter.loadColumns(affyFile, Array(ProbeID, column))

  val foreignToAffy: Map[String, Seq[String]] = {
    val raw = (for {
      Seq(affy, foreigns) <- data
      foreign <- column.expandList(foreigns)
      pair = (affy, foreign)
    } yield pair)

    raw.groupBy(_._2).map { case (foreign, data) => (foreign -> data.map(_._1)) }
  }

  println("ID conversion sample entries: ")
  for (d <- foreignToAffy take 10) println(d)

}
