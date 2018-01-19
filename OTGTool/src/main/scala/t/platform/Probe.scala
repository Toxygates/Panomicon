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

//TODO reconsider which members should be standard for a T application in general
//Most of these are used by Toxygates, but not by Tritigate
case class Probe(val identifier: String, override val name: String = "",
  val titles: Iterable[String] = Set(),
  val proteins: Iterable[Protein] = Set(),
  val genes: Iterable[Gene] = Set(),
  val symbols: Iterable[Gene] = Set(),
  val platform: String = "") extends StoredBioObject[Probe] {

  def symbolStrings = symbols.map(_.symbol)

  //GenBioObject overrides hashCode
  override def equals(other: Any): Boolean = other match {
    case Probe(id, _, _, _, _, _, _) => id == identifier
    case _                           => false
  }

  def pack = Probes.defaultPrefix + "/" + identifier

  //TODO consider best location of this
  def isMiRna: Boolean = {
    identifier.contains("-miR-") ||
      identifier.contains("-let-")
  }
}

case class OrthologousProbe(id: String, platformId: String = "")

case class OrthologGroup(title: String, probes: Iterable[OrthologousProbe]) {
  def id = title // TODO reconsider
  def subIds: Iterable[String] = probes.map(_.id)
  def platforms: Iterable[String] = probes.map(_.platformId).toSet
}
