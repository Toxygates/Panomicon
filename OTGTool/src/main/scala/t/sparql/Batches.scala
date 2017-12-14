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

import java.io._

import Triplestore.tPrefixes
import t.db.Metadata
import t.TriplestoreConfig
import t.db.Sample
import t.util.TempFiles

object Batches extends RDFClass {
  val defaultPrefix: String = s"$tRoot/batch"
  val memberRelation = "t:visibleIn"
  val itemClass: String = "t:batch"

  /**
   * Construct RDF data as a TTL file to represent the given metadata.
   */
  def metadataToTTL(md: Metadata, tempFiles: TempFiles, samples: Iterable[Sample]): File = {
    val f = tempFiles.makeNew("metadata", "ttl")
    val fout = new BufferedWriter(new FileWriter(f))
    for (s <- samples) {
      fout.write(s"<${Samples.defaultPrefix}/${s.identifier}>\n")
      fout.write(s"  a <$tRoot/sample>; rdfs:label" + "\"" + s.identifier + "\"; \n")
      val params = md.attributes(s).map(
        p => s"<$tRoot/${p._1.id}> " + "\"" + TRDF.escape(p._2) + "\"")
      fout.write(params.mkString(";\n  ") + ".")
      fout.write("\n\n")
    }

    fout.close()
    f
  }

  def context(title: String) = defaultPrefix + "/" + title
}

/**
 * A way of grouping batches.
 */
abstract class BatchGroups(config: TriplestoreConfig) extends ListManager(config) {
  def memberRelation: String
  def groupPrefix: String
  def batchPrefix = Batches.defaultPrefix
  def groupClass: String

  def memberRelation(name: String, group: String): String =
    s"<$batchPrefix/$name> $memberRelation <$groupPrefix/$group>"

  def addMember(name: String, instance: String): Unit =
    triplestore.update(s"$tPrefixes\n INSERT DATA { ${memberRelation(name, instance)} . } ")

  def removeMember(name: String, instance: String): Unit =
    triplestore.update(s"$tPrefixes\n DELETE DATA { ${memberRelation(name, instance)} . } ")

  def listGroups(name: String): Seq[String] =
    triplestore.simpleQuery(s"""$tPrefixes
        |SELECT ?gl WHERE {
        |  <$batchPrefix/$name> $memberRelation ?gr .
        |  ?gr rdfs:label ?gl; a $groupClass .
        |}""".stripMargin)
}

/**
 * TODO inheriting BatchGroups (for instance membership management)
 * and forwarding methods makes the
 * public interface really big.
 */
class Batches(config: TriplestoreConfig) extends BatchGroups(config) {
  import Triplestore._
  val memberRelation = Batches.memberRelation

  def itemClass = Batches.itemClass
  def defaultPrefix: String = Batches.defaultPrefix
  def groupPrefix = Instances.defaultPrefix
  def groupClass = Instances.itemClass

  def accessRelation(batch: String, instance: String): String =
    memberRelation(batch, instance)

  def enableAccess(name: String, instance: String): Unit =
    addMember(name, instance)

  def disableAccess(name: String, instance: String): Unit =
    removeMember(name, instance)

  def listAccess(name: String): Seq[String] = listGroups(name)

  def numSamples: Map[String, Int] = {
    val r = triplestore.mapQuery(s"""$tPrefixes
      |SELECT (count(distinct ?s) as ?n) ?l WHERE {
      |  GRAPH ?x {
      |    ?s a t:sample .
      |  } ?x rdfs:label ?l ; a $itemClass.
      |} GROUP BY ?l""".stripMargin)
    if (r(0).keySet.contains("l")) {
      Map() ++ r.map(x => x("l") -> x("n").toInt)
    } else {
      // no records
      Map()
    }
  }

  def datasets: Map[String, String] = {
    Map() ++ triplestore.mapQuery(s"""$tPrefixes
        |SELECT ?l ?dataset WHERE {
        |  ?item a $itemClass; rdfs:label ?l ;
        |  ${Datasets.memberRelation} ?ds. ?ds a ${Datasets.itemClass}; rdfs:label ?dataset .
        |} """.stripMargin).map(x => {
      x("l") -> x("dataset")
    })
  }

  def samples(batch: String): Iterable[String] = {
    val prefix = Samples.defaultPrefix
    triplestore.simpleQuery(s"$tPrefixes\nSELECT ?l WHERE " +
      s"{ graph <$defaultPrefix/$batch> { ?x a t:sample ; rdfs:label ?l } }")
  }

  override def delete(name: String): Unit = {
    super.delete(name)
    triplestore.update(s"$tPrefixes\n " +
      s"DROP GRAPH <$defaultPrefix/$name>")
  }

}
