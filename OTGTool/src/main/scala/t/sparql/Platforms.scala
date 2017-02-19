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

package t.sparql

import t.TriplestoreConfig
import t.platform.ProbeRecord
import t.util.TempFiles
import t.platform.BioParameter
import t.platform.BioParameters

object Platforms extends RDFClass {
  def itemClass: String = "t:platform"
  def defaultPrefix: String = s"$tRoot/platform"

  val platformType = "t:platformType"
  val biologicalPlatform = "t:biological"

  def context(name: String) = defaultPrefix + "/" + name
}

class Platforms(config: TriplestoreConfig) extends ListManager(config) with TRDF {
  import Triplestore._
  import Platforms._

  def itemClass = Platforms.itemClass
  def defaultPrefix = Platforms.defaultPrefix

  def redefine(name: String, comment: String, biological: Boolean,
      definitions: Iterable[ProbeRecord]): Unit = {
    delete(name) //ensure probes are removed
    addWithTimestamp(name, comment)

    if (biological) {
      ts.update(s"$tPrefixes\n insert data { <$defaultPrefix/$name> $platformType $biologicalPlatform. }")
    }

    val probes = new Probes(config)

    val tempFiles = new TempFiles()
    try {
      for (g <- definitions.par.toList.grouped(1000)) {
        val ttl = Probes.recordsToTTL(tempFiles, name, g)
        ts.addTTL(ttl, Platforms.context(name))
      }
    } finally {
      tempFiles.dropAll
    }
  }

  //TODO test
  def isBiological(name: String): Boolean =
    platformTypes.get(name) == Some(biologicalPlatform)

  /**
   * Note, the map may only be partially populated
   */
  def platformTypes: Map[String, String] = {
    Map() ++ ts.mapQuery(s"$tPrefixes select ?l ?type where { ?item a $itemClass; rdfs:label ?l ; " +
      s"$platformType ?type } ").map(x => {
      x("l") -> x("type")
    })
  }

  private def removeProbeAttribPrefix(x: String) =
    if (x.startsWith(Probes.probeAttributePrefix + "/")) {
      x.drop(Probes.probeAttributePrefix.size + 1)
    } else {
      x
    }

  /**
   * Obtain the bio-parameters in all bio platforms
   */
  def bioParameters: BioParameters = {
    val timeout: Int = 60000

    //TODO test this
    val attribs = ts.mapQuery(s"""$tPrefixes
        |SELECT ?id ?key ?value WHERE {
        |  ?p $platformType $biologicalPlatform.
        |  GRAPH ?p {
        |    ?probe a t:probe; rdfs:label ?id; ?key ?value.
        |  }
        |}""".stripMargin, timeout).map(x => (x("id"),
            removeProbeAttribPrefix(x("key")) -> x("value"))).groupBy(_._1)

    val attribMaps = for (
      (id, values) <- attribs;
      kvs = Map() ++ values.map(_._2)
    ) yield (id -> kvs)

    val bps = ts.mapQuery(s"""$tPrefixes
      |SELECT ?id ?desc ?sec ?type ?lower ?upper WHERE {
      |  ?p $platformType $biologicalPlatform.
      |  GRAPH ?p {
      |    ?probe a t:probe; rdfs:label ?id; t:label ?desc; t:type ?type.
      |    OPTIONAL { ?probe t:lowerBound ?lower; t:upperBound ?upper. }
      |    OPTIONAL { ?probe t:section ?sec. }
      |   }
      |}""".stripMargin, timeout)

    val bpcons = bps.map(x => BioParameter(x("id"), x("desc"), x("type"),
      x.get("sec"),
      x.get("lower").map(_.toDouble), x.get("upper").map(_.toDouble),
      attribMaps.get(x("id")).getOrElse(Map())))

    new BioParameters(Map() ++ bpcons.map(b => b.key -> b))
  }

  override def delete(name: String): Unit = {
    super.delete(name)
    ts.update(s"$tPrefixes\n " +
      s"drop graph <$defaultPrefix/$name>")
  }

}
