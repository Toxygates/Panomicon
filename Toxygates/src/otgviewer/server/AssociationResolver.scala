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

import t.platform.Species._
import otg.sparql._
import t.common.shared.AType
import t.model.SampleClass
import t.platform.Probe
import t.sparql._
import t.sparql.secondary._
import t.sparql.toBioMap
import t.viewer.server.intermine.IntermineConnector
import t.viewer.server.intermine.TargetmineColumns

/**
 * Association resolver for Open TG-GATEs-specific associations.
 */
class AssociationResolver(probeStore: OTGProbes,
    sampleStore: OTGSamples,
    b2rKegg: B2RKegg,
    uniprot: Uniprot,
    chembl: ChEMBL,
    drugBank: DrugBank,
    targetmine: Option[IntermineConnector],
    sc: SampleClass, types: Array[AType],
     _probes: Iterable[String]) extends
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

    def getTargeting(sc: SampleClass, from: CompoundTargets)
      (implicit sf: SampleFilter): MMap[Probe, Compound] = {
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

    def resolveMiRNA(probes: Iterable[Probe]): BBMap = {
      val (isMirna, isNotMirna) = probes.partition(_.isMiRna)

      val immediateLookup = probeStore.mirnaAccessionLookup(isMirna)

      val viaGeneAnnotation = toBioMap(isNotMirna, (_: Probe).genes) combine
          mirnaResolver.forGenes(probes.flatMap(_.genes))

      immediateLookup ++ viaGeneAnnotation
    }

    lazy val mirnaResolver = TargetmineColumns.miRNA(targetmine.get)

  override def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe])
    (implicit sf: SampleFilter): BBMap = {
    import AType._
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
      case MiRNA      => resolveMiRNA(probes)
      case _          => super.associationLookup(at, sc, probes)
    }
    }
  }
