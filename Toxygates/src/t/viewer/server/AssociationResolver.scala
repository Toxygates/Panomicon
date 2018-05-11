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

package t.viewer.server

import scala.collection.{ Set => CSet }

import t.common.server.ScalaUtils.gracefully
import t.common.shared.AType
import t.db.DefaultBio
import t.model.SampleClass
import t.platform.Probe
import t.sparql._
import t.sparql.Probes
import t.sparql.SampleFilter
import t.sparql.secondary._
import t.viewer.server.Conversions._

import t.viewer.shared.Association
import t.viewer.shared.Association._

/**
 * Helper class to look up probe associations from a variety of sources
 * effectively.
 *
 * @param probeStore source of information about probes
 * @param b2rKegg source of information about KEGG pathways
 * @param sc sample class/filter
 * @param types association types to look up
 * @param _probes probes to look up information for
 */
class AssociationResolver(probeStore: Probes,
    b2rKegg: B2RKegg,
    sc: SampleClass, types: Array[AType],
    _probes: Iterable[String]) {

  //Look up all core associations first.
  val aprobes = probeStore.withAttributes(_probes.map(Probe(_)))

  lazy val proteins = toBioMap(aprobes, (_: Probe).proteins)

  /**
   * Look up a single association type for the given set of probes.
   */
  def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe])
    (implicit sf: SampleFilter): BBMap = {
    import AType._
    at match {
      // The type annotation :BBMap is needed on at least one (!) match pattern
      // to make the match statement compile. TODO: research this
      case Uniprot => proteins: BBMap
      case GO      => probeStore.goTerms(probes)
      case KEGG =>
        toBioMap(probes, (_: Probe).genes) combine
          b2rKegg.forGenes(probes.flatMap(_.genes))
//      case Enzymes =>
//        val sp = asSpecies(sc)
//        b2rKegg.enzymes(probes.flatMap(_.genes), sp)
      case _ => throw new Exception("Unexpected annotation type")
    }
  }

  val emptyVal = CSet(DefaultBio("error", "(Timeout or error)", None))
  val errorVals = Map() ++ aprobes.map(p => (Probe(p.identifier) -> emptyVal))

  def queryOrEmpty[T](f: () => BBMap): BBMap = {
    gracefully(f, errorVals)
  }

  private def lookupFunction(t: AType)(implicit sf: SampleFilter): BBMap =
    queryOrEmpty(() => associationLookup(t, sc, aprobes))

  def standardMapping(m: BBMap): MMap[String, (String, String, Option[String])] =
    m.mapKeys(_.identifier).mapInnerValues(p => (p.name, p.identifier, p.additionalInfo))

  def resolve(implicit sf: SampleFilter): Array[Association] = {
    val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)(sf)))).seq
    m1.map(p => new Association(p._1, convertAssociations(p._2))).toArray
  }
}
