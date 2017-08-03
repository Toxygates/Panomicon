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

package t.db.file

import friedrich.util.formats.TSVFile
import t.db.ParameterSet
import t.db.Sample
import t.db.Metadata
import t.Factory
import t.model.sample.AttributeSet
import t.model.sample.Attribute
import t.model.sample.Helpers._
import scala.collection.JavaConversions._


/**
 * Metadata that is read from a TSV file.
 */
object TSVMetadata {
  def apply(fact: Factory, file: String, attributes: AttributeSet): Metadata = {
    val metadata: Map[String, Seq[String]] = {
      val columns = TSVFile.readMap("", file)
      columns.flatMap(x => {
        val lc = x._1.toLowerCase().trim
        //Normalise the upper/lowercase-ness and remove unknown columns
        val trimmed = x._2.map(_.trim)
        attributes.byIdLowercase.get(lc).map(_.id -> trimmed)
      })
    }

    val req = attributes.getRequired().map(_.id).map(_.toLowerCase)
    val neColumns = req.filter(!metadata.keySet.contains(_))
    if (!neColumns.isEmpty) {
      println(s"The following columns are missing in $file: $neColumns")
      throw new Exception("Missing columns in metadata")
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
    val attributes: AttributeSet) extends Metadata {

  val requiredColumns = attributes.getRequired.map(_.id.toLowerCase)

  def samples: Iterable[Sample] = {
    val ids = metadata("sample_id")
    ids.map(Sample(_))
  }

  protected lazy val idxLookup = Map() ++ metadata("sample_id").zipWithIndex

  protected def getIdx(s: Sample): Int = {
    idxLookup.get(s.identifier) match {
      case Some(i) => i
      case _ =>
        throw new Exception(s"Sample (${s.sampleId}) not found in metadata")
    }
  }

  override def parameters(s: Sample): Seq[(Attribute, String)] = {
    val idx = getIdx(s)
    metadata.map(column => (attributes.byId(column._1), column._2(idx))).toSeq
  }

  def parameterValues(identifier: String): Set[String] =
    metadata(identifier).toSet

  override def parameter(s: Sample, identifier: String): Option[String] = {
    val idx = getIdx(s)
    Some(metadata(identifier)(idx))
  }

  def mapParameter(fact: Factory, key: String, f: String => String): Metadata = {
    val nm = metadata + (key -> metadata(key).map(f))
    fact.metadata(nm, attributes)
  }
}
