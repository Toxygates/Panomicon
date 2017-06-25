package t.viewer.server

import t.viewer.shared.Association
import t.db.DefaultBio
import t.sparql.SampleFilter
import t.common.shared.AType
import t.viewer.shared.Association
import t.common.shared.SampleClass
import t.platform.Probe
import t.sparql.Probes
import Association._
import t.sparql._
import t.sparql.secondary._
import t.viewer.server.Conversions._
import t.common.server.ScalaUtils.gracefully
import scala.collection.{Set => CSet}

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
    (implicit sf: SampleFilter): BBMap =
    at match {
      // The type annotation :BBMap is needed on at least one (!) match pattern
      // to make the match statement compile. TODO: research this
      case _: AType.Uniprot.type   => proteins: BBMap
      case _: AType.GO.type        => probeStore.goTerms(probes)

      case _: AType.KEGG.type =>
        toBioMap(probes, (_: Probe).genes) combine
          b2rKegg.forGenes(probes.flatMap(_.genes))
//      case _: AType.Enzymes.type =>
//        val sp = asSpecies(sc)
//        b2rKegg.enzymes(probes.flatMap(_.genes), sp)
      case _ => throw new Exception("Unexpected annotation type")
    }

  val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
  val errorVals = Map() ++ aprobes.map(p => (Probe(p.identifier) -> emptyVal))

  def queryOrEmpty[T](f: () => BBMap): BBMap = {
    gracefully(f, errorVals)
  }

  private def lookupFunction(t: AType)(implicit sf: SampleFilter): BBMap =
    queryOrEmpty(() => associationLookup(t, sc, aprobes))

  def standardMapping(m: BBMap): MMap[String, (String, String)] =
    m.mapKeys(_.identifier).mapInnerValues(p => (p.name, p.identifier))

  def resolve(implicit sf: SampleFilter): Array[Association] = {
    val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)(sf)))).seq
    m1.map(p => new Association(p._1, convertPairs(p._2))).toArray
  }
}
