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

package otgviewer.server

import otg.sparql._
import t._
import t.common.shared.AType
import t.db.DefaultBio
import t.model.SampleClass
import t.platform.Probe
import t.platform.Species._
import t.platform.mirna.TargetTable
import t.sparql._
import t.sparql.secondary._
import t.sparql.toBioMap
import t.viewer.server.Conversions._
import t.viewer.server.intermine.IntermineConnector
import t.viewer.server.intermine.TargetmineColumns
import t.viewer.shared.mirna.MirnaSource
import t.platform.mirna.MiRDBConverter

/**
 * Association resolver for Open TG-GATEs-specific associations.
 * TODO: this is a little complex, should be split up or arranged differently
 */
class AssociationResolver(probeStore: OTGProbes,
    sampleStore: OTGSamples,
    platforms: t.viewer.server.Platforms,
    b2rKegg: B2RKegg,
    uniprot: Uniprot,
    chembl: ChEMBL,
    drugBank: DrugBank,
    mirnaTable: TargetTable,
    sidePlatform: Option[String], //Needed for MiRNA resolution
    sc: SampleClass, types: Array[AType],
     _probes: Iterable[String])(implicit sf: SampleFilter) extends
     t.viewer.server.AssociationResolver(probeStore, b2rKegg, sc, types, _probes) {

      //    val sp = asSpecies(sc)
    //orthologous proteins if needed
    lazy val oproteins = {

      val r = if ((types.contains(AType.Chembl) ||
        types.contains(AType.Drugbank) ||
        types.contains(AType.OrthProts))
        //      && (sp != Human)
        && false // Not used currently due to performance issues!
        ) {
        // This always maps to Human proteins as they are assumed to contain the most targets
        val r = proteins combine ((ps: Iterable[Protein]) => uniprot.orthologsFor(ps, Human))
        r
      } else {
        emptyMMap[Probe, Protein]()
      }
      println(r.allValues.size + " oproteins")
      r
    }

    def getTargeting(sc: SampleClass, from: CompoundTargets): MMap[Probe, Compound] = {
      val expected = sampleStore.compounds(SampleClassFilter(sc).filterAll).map(Compound.make(_))

      //strictly orthologous
      val oproteinVs = oproteins.allValues.toSet -- proteins.allValues.toSet
      val allProteins = proteins union oproteins
      val allTargets = from.targetingFor(allProteins.allValues, expected)

      allProteins combine allTargets.map(x => if (oproteinVs.contains(x._1)) {
        (x._1 -> x._2.map(c => c.copy(name = c.name + " (inf)")))
      } else {
        x
      })
    }

  /**
   * Look up miRNA-mRNA associations (by default from mRNA)
   */
  def resolveMiRNAInner(probes: Iterable[Probe],
    fromMirna: Boolean): MMap[Probe, DefaultBio] = {
    val species = asSpecies(sc)
    val sizeLimit = Some(1000)

    try {
      if (mirnaTable.size == 0) {
        Console.err.println("Warning: no mirnaTable available, association lookups will fail")
      }
      //Note: we might perform this filtering once and store it in the matrix state
      val filtTable = mirnaTable.speciesFilter(species)
      println(s"Lookup from miRNA table of size ${filtTable.size}, species: $species")

      //Note: we might unify this lookup with the "aprobes" mechanism
      val lookedUp = platforms.resolve(probes.map(_.identifier).toSeq)

      if (!sidePlatform.isDefined) {
        throw new Exception("The side platform of the association resolver must be set for miRNA lookup to work.")
      }

      val data = filtTable.associationLookup(lookedUp, fromMirna,
        probeStore.platformsAndProbes(sidePlatform.get), sizeLimit)

      if (sizeLimit.map(_ <= data.size).getOrElse(false)) {
        sizeLimitExceeded = true
      }
      data

    } catch {
      case e: Exception =>
        e.printStackTrace()
        emptyMMap()
    }
  }

  def resolveMiRNA(probes: Iterable[Probe], fromMirna: Boolean): BBMap = {
    import t.platform.mirna._
    val (isMirna, isNotMirna) = probes.partition(isMiRNAProbe)

    if (!fromMirna) {
      val immediateLookup = probeStore.mirnaAccessionLookup(isMirna)
      val resolved = resolveMiRNAInner(isNotMirna, false)
      immediateLookup ++ resolved
    } else {
      resolveMiRNAInner(isMirna, true)
    }
  }

  override def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe]): BBMap = {
    import t.common.shared.AType._
    at match {
      case GOMF       => probeStore.mfGoTerms(probes)
      case GOBP       => probeStore.bpGoTerms(probes)
      case GOCC       => probeStore.ccGoTerms(probes)
      case OrthProts  => oproteins
      case Chembl     => getTargeting(sc, chembl)
      case Drugbank   => getTargeting(sc, drugBank)
      case RefseqTrn  => probeStore.refseqTrnLookup(probes)
      case RefseqProt => probeStore.refseqProtLookup(probes)
      case Ensembl    => probeStore.ensemblLookup(probes)
      case EC         => probeStore.ecLookup(probes)
      case Unigene    => probeStore.unigeneLookup(probes)
      case MiRNA      => resolveMiRNA(probes, false)
      case MRNA       => resolveMiRNA(probes, true)
      case _          => super.associationLookup(at, sc, probes)
    }
    }
  }
