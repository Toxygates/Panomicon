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

package t.viewer.server

import t.common.shared.AType
import t.common.shared.AType._
import t.db.DefaultBio
import t.model.SampleClass
import t.platform.Probe
import t.platform.mirna.TargetTable
import t.sparql.secondary._
import t.sparql.{toBioMap, _}
import t.viewer.server.Conversions._
import t.viewer.server.network.NetworkController
import t.viewer.shared.Association

import scala.collection.{Set => CSet}

class DrugTargetResolver(sampleStore: SampleStore, chembl: ChEMBL,
                         drugBank: DrugBank) {

  def lookup: AssociationLookup = {
    case (Drugbank, sc, sf, probes) => getTargeting(sc, drugBank, probes, sf)
    case (Chembl, sc, sf, probes)   => getTargeting(sc, chembl, probes, sf)
  }

  def getTargeting(sc: SampleClass, from: CompoundTargets,
                   probes: Iterable[Probe], sf: SampleFilter
                  ): MMap[Probe, Compound] = {
    val expected = sampleStore.compounds(SampleClassFilter(sc).filterAll, sf).map(Compound.make(_))

    val proteins = toBioMap(probes, (_: Probe).proteins)

    val allTargets = from.targetingFor(proteins.allValues, expected)
    proteins combine allTargets
  }
}

class MirnaResolver(probeStore: ProbeStore, platforms: t.viewer.server.PlatformRegistry, mirnaTable: TargetTable,
                    mainPlatform: Option[String], sidePlatform: Option[String]) {

  def lookup: AssociationLookup = {
      case (MiRNA, sc, _, probes)      => resolveMiRNA(sc, probes, false)
      case (MRNA, sc, _, probes)       => resolveMiRNA(sc, probes, true)
  }

  /**
   * Look up miRNA-mRNA associations (by default from mRNA)
   */
  def resolveMiRNAInner(sc: SampleClass, probes: Iterable[Probe],
    fromMirna: Boolean): MMap[Probe, DefaultBio] = {
    val species = asSpecies(sc)

    if (mirnaTable.isEmpty) {
      Console.err.println("Target table is empty; no mRNA-miRNA associations will be found")
    }
    //Note: we might perform this filtering once and store it in the matrix state
    val filtTable = species.map(mirnaTable.speciesFilter(_)).getOrElse(mirnaTable)
    println(s"Lookup from miRNA table of size ${filtTable.size}, species: $species")

    //Note: we might unify this resolution with the "aprobes" mechanism
    val resolved = platforms.resolve(mainPlatform, probes.map(_.identifier).toSeq)

    val platform = sidePlatform.map(probeStore.platformsAndProbes)
    // Note: we pass None for the sizeLimit argument here because we have no way of
    // getting the sizeLimit argument from AssociationResolver. In the future it may
    // make sense to add a sizeLimit argument to AssociationLookup
    val data = filtTable.associationLookup(resolved, fromMirna,
      platform, None)

    val total = data.values.map(_.size).sum
    data
  }

  def resolveMiRNA(sc: SampleClass, probes: Iterable[Probe], fromMirna: Boolean): BBMap = {
    import t.platform.mirna._
    val (isMirna, isNotMirna) = probes.partition(isMiRNAProbe)

    if (!fromMirna) {
      val immediateLookup = probeStore.mirnaAccessionLookup(isMirna)
      val resolved = resolveMiRNAInner(sc, isNotMirna, false)
      immediateLookup ++ resolved
    } else {
      resolveMiRNAInner(sc, isMirna, true)
    }
  }
}

/**
 * The association resolver looks up probe associations based on the AType enum.
 * Subresolvers provide partial functions that perform the resolution.
 */
class AssociationResolver(probeStore: ProbeStore,
                          sampleStore: SampleStore,
                          b2rKegg: B2RKegg) {

  val mainResolver: AssociationLookup = {
    case (GOMF, _, _, probes)       => probeStore.mfGoTerms(probes)
    case (GOBP, _, _, probes)       => probeStore.bpGoTerms(probes)
    case (GOCC, _, _, probes)       => probeStore.ccGoTerms(probes)
    case (RefseqTrn, _, _, probes)  => probeStore.refseqTrnLookup(probes)
    case (RefseqProt, _, _, probes) => probeStore.refseqProtLookup(probes)
    case (Ensembl, _, _, probes)    => probeStore.ensemblLookup(probes)
    case (EC, _, _, probes)         => probeStore.ecLookup(probes)
    case (Unigene, _, _, probes)    => probeStore.unigeneLookup(probes)
    case (Uniprot, _, _, probes)    => toBioMap(probes, (_: Probe).proteins)
    case (KEGG, _, _, probes) =>
      toBioMap(probes, (_: Probe).genes) combine
        b2rKegg.forGenes(probes.flatMap(_.genes))
  }

  /**
   * Resolve associations for a single type.
   * @param at The association being resolved
   * @param sc SampleClass that associations are being resolved for
   * @param sf SampleFilter that the user currently can view
   * @param probes Probes whose associations are being obtained
   * @param extraResolvers Additional resolvers to also use
   */
  def associationLookup(at: AType,  sc: SampleClass, sf: SampleFilter,
                        probes: Iterable[Probe], extraResolvers: Iterable[AssociationLookup]): BBMap = {
    val resolvers = (Seq(mainResolver) ++ extraResolvers).reduce(_ orElse _)

    resolvers.lift(at, sc, sf, probes) match {
      case Some(r) => r
      case None =>  throw new Exception("Unexpected annotation type")
    }
  }

  val emptyVal = CSet(DefaultBio("error", "(Timeout or error)", None))
  def errorVals(probes: Iterable[Probe]) = Map() ++
    probes.map(p => (Probe(p.identifier) -> emptyVal))

  def errorAssoc(t: AType, probes: Iterable[Probe], sizeLimit: Int) =
    new Association(t,
    convertAssociations(errorVals(probes)), sizeLimit, false)

  def resolve(types: Iterable[AType], sc: SampleClass, sf: SampleFilter,
    probes: Iterable[String], extraResolvers: Iterable[AssociationLookup] = Seq(),
    sizeLimit: Int = 10): Array[Association] = {

    //Look up all core associations first.
    //Note: this might not be needed - platformsCache might do a better job
    val aprobes = probeStore.withAttributes(probes.map(Probe(_)))

    val allAssociations = for {
      t <- types.par
      // catch exceptions here to prevent them from stopping the whole
      // parallel loop
      association = try {
        val data = associationLookup(t, sc, sf, aprobes, extraResolvers)
        new Association(t, convertAssociations(data), sizeLimit, true)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          errorAssoc(t, aprobes, sizeLimit)
      }
    } yield association
    allAssociations.seq.toArray
  }
}

/**
 * Holds references to the various association sources and marshals lookup requests.
 */
class AssociationMasterLookup(probeStore: ProbeStore, sampleStore: SampleStore,
                              sampleFilter: SampleFilter) {
  val chembl = new ChEMBL()
  val drugBank = new DrugBank()

  val triplestore = sampleStore.triplestore
  lazy val drugTargetResolver = new DrugTargetResolver(sampleStore, chembl, drugBank).lookup
  lazy val b2rKegg: B2RKegg = new B2RKegg(triplestore.conn)
  lazy val associationResolver =  new AssociationResolver(probeStore, sampleStore, b2rKegg)
  lazy val platformsCache = new PlatformRegistry(probeStore)

  /**
   * Basic association lookup
   */
  def doLookup(sc: SampleClass, types: Array[AType], probes: Array[String],
               sizeLimit: Int): Array[Association] = {
    val customResolvers = Seq(drugTargetResolver)
    associationResolver.resolve(types, sc, sampleFilter, probes, customResolvers, sizeLimit)
  }

  /**
   * Association lookup for networks
   *
   * Note: might reconsider the correct location for this variant, since it contains logic
   * that relates only to networks/target tables
   */
  def doLookupForNetwork(sc: SampleClass, types: Array[AType], probes: Array[String],
                         sizeLimit: Int,
                       network: Option[NetworkController], targetTable: TargetTable): Array[Association] = {
    val mainPlatform = network.map(_.managedMatrix.params.platform)
    val sidePlatform = network.map(_.sideMatrix.params.platform)
    val mirnaRes = new MirnaResolver(probeStore, platformsCache, targetTable, mainPlatform, sidePlatform)
    val customResolvers = Seq(drugTargetResolver, mirnaRes.lookup)
    associationResolver.resolve(types, sc, sampleFilter, probes, customResolvers, sizeLimit)
  }
}