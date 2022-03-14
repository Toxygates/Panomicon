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

package t.sparql

import java.io._
import java.util.Date
import Triplestore.tPrefixes
import t.TriplestoreConfig
import t.db._
import t.model.sample.AttributeSet
import t.util.TempFiles
import scala.collection.JavaConverters._

object BatchStore extends RDFClass {
  val defaultPrefix: String = s"$tRoot/batch"
  val memberRelation = "t:visibleIn"
  val itemClass: String = "t:batch"

  /**
   * Construct a TTL file(RDF) to represent sample attributes.
   * @param md
   * @param tempFiles
   * @return
   */
  def attributesToTTL(attributes: AttributeSet, tempFiles: TempFiles): File = {
    //These may not be redefined
    val predefinedAttributes = AttributeSet.newMinimalSet().getRequired.asScala.map(_.id()).toSet
    val file = tempFiles.makeNew("metadata", "ttl")
    val fout = new BufferedWriter(new FileWriter(file))

    for {
      attr <- attributes.getAll.asScala
      if ! predefinedAttributes.contains(attr.id())
    } {
      fout.write(t.sparql.TTLfilePrefixes)
      fout.write(s"<${ProbeStore.defaultPrefix}/${attr.id()}>")
      fout.write(s"  a ${ProbeStore.itemClass}; rdfs:label " + "\"" + attr.id() + "\";")
      fout.write("  t:label \"" + attr.title() + "\"; t:type " +
        (if(attr.isNumerical) "\"numerical\"" else "\"string\""))
      Option(attr.section()) match {
        case Some(sec) =>
          fout.write(";  t:section \"" + sec +"\".")
        case _ => fout.write(".")
      }
    }
    fout.close()
    file
  }

  /**
   * Construct a TTL file (RDF) to represent samples in the given metadata.
   */
  def metadataSamplesToTTL(md: Metadata, tempFiles: TempFiles, samples: Iterable[Sample]): File = {
    val file = tempFiles.makeNew("metadata", "ttl")
    val fout = new BufferedWriter(new FileWriter(file))
    for (s <- samples) {
      fout.write(t.sparql.TTLfilePrefixes)
      fout.write(s"<${SampleStore.defaultPrefix}/${s.identifier}>\n")
      fout.write(s"  a ${SampleStore.itemClass}; rdfs:label" + "\"" + s.identifier + "\"; \n")
      val params = md.sampleAttributes(s).map(
        p => s"<$tRoot/${p._1.id}> " + "\"" + TRDF.escape(p._2) + "\"")
      fout.write(params.mkString(";\n  ") + ".")
      fout.write("\n\n")
    }

    fout.close()
    file
  }

  def context(title: String) = defaultPrefix + "/" + title
}

/**
 * A way of grouping batches.
 */
trait BatchGrouping {
  this: ListManager[_] =>

  def memberRelation: String
  def groupPrefix: String
  def batchPrefix = BatchStore.defaultPrefix
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

case class Batch(id: String, timestamp: Date, comment: String, publicComment: String,
                 dataset: String, numSamples: Int) {
  def toBatchManager(instances: List[String]) =
    t.manager.BatchManager.Batch(id, comment, Some(instances), Some(dataset))

}

/*
 * Note: inheriting BatchGroups (to manage instance membership)
 * makes the public interface of this class large. We may want to use composition instead.
 */

class BatchStore(config: TriplestoreConfig) extends ListManager[Batch](config) with BatchGrouping {
  import Triplestore._
  val memberRelation = BatchStore.memberRelation

  def itemClass = BatchStore.itemClass
  def defaultPrefix: String = BatchStore.defaultPrefix
  def groupPrefix = InstanceStore.defaultPrefix
  def groupClass = InstanceStore.itemClass

  def accessRelation(batch: String, instance: String): String =
    memberRelation(batch, instance)

  def enableAccess(name: String, instance: String): Unit =
    addMember(name, instance)

  def disableAccess(name: String, instance: String): Unit =
    removeMember(name, instance)

  def listAccess(name: String): Seq[String] = listGroups(name)

  def getSampleCounts(): Map[String, Int] = {
    val r = triplestore.mapQuery(s"""$tPrefixes
      |SELECT (count(distinct ?s) as ?n) ?l WHERE {
      |  GRAPH ?x {
      |    ?s a t:sample .
      |  } ?x rdfs:label ?l ; a $itemClass.
      |} GROUP BY ?l""".stripMargin)
    if (r.isEmpty) {
      Map()
    } else if (r(0).keySet.contains("l")) {
      Map() ++ r.map(x => x("l") -> x("n").toInt)
    } else {
      Map()
    }
  }

  def getDatasets(): Map[String, String] = {
    Map() ++ triplestore.mapQuery(s"""$tPrefixes
        |SELECT ?l ?dataset WHERE {
        |  ?item a $itemClass; rdfs:label ?l ;
        |  ${DatasetStore.memberRelation} ?ds. ?ds a ${DatasetStore.itemClass}; rdfs:label ?dataset .
        |} """.stripMargin).map(x => {
      x("l") -> x("dataset")
    })
  }

  def getPlatforms(batch: String): Iterable[String] = {
    val prefix = SampleStore.defaultPrefix
    triplestore.simpleQuery(s"$tPrefixes\nSELECT DISTINCT ?pl WHERE " +
      s"{ GRAPH <$defaultPrefix/$batch> { ?x a t:sample; t:platform_id ?pl } }")

  }

  def getSamples(batch: String): Iterable[SampleId] = {
    val prefix = SampleStore.defaultPrefix
    triplestore.simpleQuery(s"$tPrefixes\nSELECT ?l WHERE " +
      s"{ GRAPH <$defaultPrefix/$batch> { ?x a t:sample ; rdfs:label ?l } }")
  }

  override def delete(name: String): Unit = {
    super.delete(name)
    triplestore.update(s"$tPrefixes\n " +
      s"DROP GRAPH <$defaultPrefix/$name>")
  }

  def deleteSamples(batch: String, samples: Iterable[Sample]): Unit = {
    val sampleIds = samples.toList.map(_.sampleId)
    triplestore.update(
      s"""|$tPrefixes\n
          |DELETE { GRAPH <$defaultPrefix/$batch> {
          |  ?x a t:sample; rdfs:label ?label; ?y ?z.
          | } }
          | WHERE { GRAPH <$defaultPrefix/$batch> {
          |   ?x a t:sample; rdfs:label ?label; ?y ?z.
          |  } FILTER(?label IN(${sampleIds.map(x => "\"" + x + "\"").mkString(",")}))
          | }
    """.stripMargin)
  }

  private def additionalFilter(instanceUri: Option[String], dataset: Option[String] = None) = {
    val r = (instanceUri match {
      case Some(uri) => s"?item $memberRelation <$uri>."
      case None => ""
    })
    dataset match {
      case Some(ds) => s"$r ?item ${DatasetStore.memberRelation} <${DatasetStore.defaultPrefix}/$ds>."
      case None => r
    }
  }

  def items(instanceUri: Option[String], dataset: Option[String]): Iterable[Batch] = {
    val instanceFilter = additionalFilter(instanceUri, dataset)
    val keyAttributes = getKeyAttributes(instanceFilter)
    val datasets = getDatasets()
    val sampleCounts = getSampleCounts()

    keyAttributes.map(x => {
      Batch(x.id, x.timestamp, x.comment, x.publicComment, datasets.getOrElse(x.id, ""),
        sampleCounts.getOrElse(x.id, 0))
    })
  }

  override def getItems(instanceUri: Option[String]) = items(instanceUri, None)

  def list(instanceUri: Option[String]): Seq[String] =
    super.getList(additionalFilter(instanceUri))

}
