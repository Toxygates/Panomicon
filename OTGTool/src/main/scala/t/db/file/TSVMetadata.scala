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

package t.db.file

import friedrich.util.formats.TSVFile
import t.db.ParameterSet
import t.db.Sample
import t.db.Metadata

/**
 * Metadata that is read from a TSV file.
 */
class TSVMetadata(file: String, parameters: ParameterSet)
extends MapMetadata(parameters) {
  protected val metadata: Map[String, Seq[String]] = {
    val columns = TSVFile.readMap("", file)
    columns.flatMap(x => {
      val lc = x._1.toLowerCase()
      //Normalise the upper/lowercase-ness and remove unknown columns
      parameters.byIdLowercase.get(lc).map(sp => sp.identifier -> x._2)
    })
  }

  val neColumns = requiredColumns.filter(!metadata.keySet.contains(_))
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
}

/**
 * Metadata based on a map indexed by column.
 */
abstract class MapMetadata(parameters: ParameterSet) extends Metadata {
  protected def metadata: Map[String, Seq[String]]

  val requiredColumns = parameters.required.map(_.identifier.toLowerCase)

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

  def parameters(s: Sample): Iterable[(t.db.SampleParameter, String)] = {
    val idx = getIdx(s)
    metadata.map(column => (parameters.byId(column._1), column._2(idx)))
  }

  def parameterValues(identifier: String): Set[String] =
    metadata(identifier).toSet

  override def parameter(s: Sample, identifier: String): String = {
    val idx = getIdx(s)
    metadata(identifier)(idx)
  }

  def mapParameter(key: String, f: String => String): MapMetadata = {
    val nm = metadata + (key -> metadata(key).map(f))
    new MapMetadata(parameters) {
      protected val metadata = nm
    }
  }
}
