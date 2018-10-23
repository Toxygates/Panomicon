/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import t.sparql.secondary.Gene
import t.sparql.secondary.Protein
import t.sparql.Probes
import t.db.StoredBioObject

object Probe {
  import Probes._
  private val split = defaultPrefix + "/"
  def unpack(uri: String) = Probe(unpackOnly(uri))
  def unpackOnly(uri: String) = uri.split(split)(1)
}

case class Probe(val identifier: String, override val name: String = "",
  //Gene titles
  val titles: Iterable[String] = Seq(),
  val proteins: Iterable[Protein] = Seq(),
  val genes: Iterable[Gene] = Seq(),
  val symbols: Iterable[String] = Seq(),
  val transcripts: Iterable[RefSeq] = Seq(),
  val platform: String = "") extends StoredBioObject[Probe] {

  def symbolStrings = symbols

  //GenBioObject overrides hashCode
  override def equals(other: Any): Boolean = other match {
    case Probe(id, _, _, _, _, _, _, _) => id == identifier
    case _                           => false
  }

  def pack = Probes.defaultPrefix + "/" + identifier
}
