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

package t.db.file

import scala.collection.JavaConverters._

import friedrich.util.formats.TSVFile
import t.Factory
import t.db._
import t.model.sample._
import t.model.sample.Helpers._

/**
 * Metadata that is read from a TSV file.
 */
object TSVMetadata {
  def ifNone[A](o: Option[A], action: => Unit): Option[A] = { if (o == None) action; o }

  def apply(fact: Factory, file: String, attributes: AttributeSet,
      warningHandler: (String) => Unit = println): Metadata = {
    val metadata: Map[String, Seq[String]] = {
      val columns = TSVFile.readMap("", file)
      Map() ++ (for {
        column <- columns
        lowerCase = column._1.toLowerCase().trim
        trimmed = column._2.map(_.trim)
        attribute <- ifNone(attributes.byIdLowercase.get(lowerCase),
            warningHandler(s"attribute $lowerCase not found"))
      } yield attribute.id -> trimmed)
    }

    val required = attributes.getRequired().asScala.map(_.id).map(_.toLowerCase)
    val missingColumns = required.filter(!metadata.keySet.contains(_))
    if (!missingColumns.isEmpty) {
      warningHandler(s"The following columns are missing in $file: $missingColumns")
      throw new Exception(s"Missing columns in metadata: ${missingColumns mkString ", "}")
    }

    var uniqueIds = Set[String]()
    //sanity check
    for (id <- metadata("sample_id")) {
      if (uniqueIds.contains(id)) {
        throw new Exception(s"Metadata error in $file. The sample '${id}' is defined twice.")
      }
      uniqueIds += id
    }
    fact.metadata(metadata, attributes)
  }
}

/**
 * Metadata based on a map indexed by column.
 */
class MapMetadata(val metadata: Map[String, Seq[String]],
    val attributeSet: AttributeSet) extends Metadata {

  val requiredColumns = attributeSet.getRequired.asScala.map(_.id.toLowerCase)

  def samples: Iterable[Sample] = {
    val ids = metadata("sample_id")
    ids.map(x => Sample(x, Map() ++ sampleAttributes(Sample(x))))
  }

  protected lazy val idxLookup = Map() ++ metadata("sample_id").zipWithIndex

  protected def getIdx(s: Sample): Int = {
    idxLookup.get(s.identifier) match {
      case Some(i) => i
      case _ =>
        throw new Exception(s"Sample (${s.sampleId}) not found in metadata")
    }
  }

  override def sampleAttributes(s: Sample): Seq[(Attribute, String)] = {
    val idx = getIdx(s)
    metadata.map(column => (attributeSet.byId(column._1), column._2(idx))).toSeq
  }

  def attributeValues(attribute: Attribute): Set[String] =
    metadata(attribute.id).toSet

  override def sampleAttribute(s: Sample, attribute: Attribute): Option[String] = {
    val idx = getIdx(s)
    metadata.get(attribute.id).map(_(idx))
  }

  def mapParameter(fact: Factory, key: String, f: String => String): Metadata = {
    val newMap = metadata + (key -> metadata(key).map(f))
    fact.metadata(newMap, attributeSet)
  }
}
