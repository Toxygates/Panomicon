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

package otg.viewer.server

import scala.collection.{ Set => CSet }

import otg.sparql._
import t._
import t.common.server.ScalaUtils.gracefully
import t.common.shared.AType
import t.common.shared.AType._
import t.db.DefaultBio
import t.model.SampleClass
import t.platform.Probe
import t.platform.mirna.TargetTable
import t.sparql._
import t.sparql.secondary._
import t.sparql.toBioMap
import t.viewer.server._
import t.viewer.server.Conversions._
import t.viewer.shared.Association


class DrugTargetResolver(sampleStore: OTGSamples, chembl: ChEMBL,
                         drugBank: DrugBank) {

  def lookup: AssociationLookup = {
    case (Drugbank, sc, sf, probes) => getTargeting(sc, drugBank, probes)(sf)
    case (Chembl, sc, sf, probes)   => getTargeting(sc, chembl, probes)(sf)
  }

  def getTargeting(sc: SampleClass, from: CompoundTargets, probes: Iterable[Probe])
    (implicit sf: SampleFilter): MMap[Probe, Compound] = {
    val expected = sampleStore.compounds(SampleClassFilter(sc).filterAll).map(Compound.make(_))

    val proteins = toBioMap(probes, (_: Probe).proteins)

    val allTargets = from.targetingFor(proteins.allValues, expected)
    proteins combine allTargets
  }
}

class MirnaResolver(probeStore: OTGProbes, platforms: t.viewer.server.Platforms, mirnaTable: TargetTable,
  sidePlatform: Option[String]) {
  @volatile var sizeLimitExceeded = false

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
    val sizeLimit = Some(1000)

    if (mirnaTable.size == 0) {
      Console.err.println("Target table is empty; no mRNA-miRNA associations will be found")
    }
    //Note: we might perform this filtering once and store it in the matrix state
    val filtTable = species.map(mirnaTable.speciesFilter(_)).getOrElse(mirnaTable)
    println(s"Lookup from miRNA table of size ${filtTable.size}, species: $species")

    //Note: we might unify this lookup with the "aprobes" mechanism
    val lookedUp = platforms.resolve(probes.map(_.identifier).toSeq)

    val platform = sidePlatform.map(probeStore.platformsAndProbes)
    val data = filtTable.associationLookup(lookedUp, fromMirna,
      platform, sizeLimit)

    if (sizeLimit.map(_ <= data.size).getOrElse(false)) {
      sizeLimitExceeded = true
    }
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
 * Subresolvers provide partial functions that perform the resolution task.
 *
 */
class AssociationResolver(probeStore: OTGProbes,
    sampleStore: OTGSamples,
    b2rKegg: B2RKegg) {

  @volatile protected var sizeLimitExceeded = false

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
   */
  def associationLookup(at: AType,  sc: SampleClass, sf: SampleFilter,
                        probes: Iterable[Probe], extraResolvers: Iterable[AssociationLookup]): BBMap = {
    val resolvers = (Seq(mainResolver) ++ extraResolvers).reduce(_ orElse _)

    if (resolvers.isDefinedAt(at, sc, sf, probes)) {
      resolvers(at, sc, sf, probes)
    } else {
      throw new Exception("Unexpected annotation type")
    }
  }


  val emptyVal = CSet(DefaultBio("error", "(Timeout or error)", None))
  def errorVals(probes: Iterable[Probe]) = Map() ++
    probes.map(p => (Probe(p.identifier) -> emptyVal))

  def errorAssoc(t: AType, probes: Iterable[Probe]) = new Association(t,
    convertAssociations(standardMapping(errorVals(probes))), false, false)

  def queryOrEmpty[T](t: AType, probes: Iterable[Probe], f: => BBMap): Association = {
    gracefully(
      new Association(t,
        convertAssociations(standardMapping(f)),
        sizeLimitExceeded, true), errorAssoc(t, probes))
  }

  //Convert a BBMap into the type we can use for building Java collections
  def standardMapping(m: BBMap): MMap[String, (String, String, Option[String])] =
    m.mapKeys(_.identifier).mapInnerValues(p => (p.name, p.identifier, p.additionalInfo))

  def resolve(types: Iterable[AType], sc: SampleClass, sf: SampleFilter,
    probes: Iterable[String],
    extraResolvers: Iterable[AssociationLookup] = Seq()): Array[Association] = {

    //Look up all core associations first.
    //TODO this might not be needed - platformsCache might do a better job
    val aprobes = probeStore.withAttributes(probes.map(Probe(_)))

    types.par.map(t => queryOrEmpty(t, aprobes,
      associationLookup(t, sc, sf, aprobes, extraResolvers))).seq.toArray
  }
}