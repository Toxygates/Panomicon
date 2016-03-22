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

package t.common.shared.probe

import t.platform.OrthologMapping

/**
 * A ProbeMapper maps probes from one domain to another.
 * Examples: transcripts to genes, genes to proteins, etc.
 * In the general case, the mapping is many-to-many.
 */
trait ProbeMapper {
  def toRange(p: String): Iterable[String]
  def toDomain(p: String): Iterable[String]

  def domain: Iterable[String]
  def range: Iterable[String]
}

class OrthologProbeMapper(mapping: OrthologMapping) extends ProbeMapper {
  var tmp = mapping.forProbe.toSeq.map(x => (x._2, x._2.mkString("/")))
  val forward = Map() ++ tmp.flatMap(m => m._1.map(x => x -> m._2))
  val reverse = Map() ++ tmp.map(m => m._2 -> m._1)

  def toRange(p: String) = List(forward(p))
  def toDomain(p: String) = reverse(p)

  def domain = forward.keySet
  def range = reverse.keySet
}
