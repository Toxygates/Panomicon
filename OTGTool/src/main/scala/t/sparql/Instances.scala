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

package t.sparql

import t.TriplestoreConfig

object Instances extends RDFClass {
  val defaultPrefix = s"$tRoot/instance"
  val itemClass = "t:instance"
  val memberRelation = Batches.memberRelation
}

class Instances(config: TriplestoreConfig) extends ListManager(config) {
  import Triplestore._
  def memberRelation = Instances.memberRelation
  def defaultPrefix = Instances.defaultPrefix
  def itemClass: String = Instances.itemClass

  def listAccess(name: String): Seq[String] = {
    ts.simpleQuery(s"$tPrefixes\n select ?bn where " +
      s"{ ?batch $memberRelation <$defaultPrefix/$name> . " +
      "?batch rdfs:label ?bn .}")
  }

  def enableAccess(name: String, batch: String): Unit =
    new Batches(config).enableAccess(batch, name)

  def disableAccess(name: String, batch: String): Unit =
    new Batches(config).disableAccess(batch, name)

  override def delete(name: String): Unit = {
    val upd = s"$tPrefixes delete { ?batch $memberRelation <$defaultPrefix/$name> } " +
      s" where { ?batch $memberRelation <$defaultPrefix/$name> }"
    super.delete(name)
  }
}
