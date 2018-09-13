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
    targetmine: Option[IntermineConnector],
    //TODO stop passing this in when all miRNA sources are unified in the TargetTable
    mirnaSources: Seq[MirnaSource],
    mirnaTable: Option[TargetTable],
    sc: SampleClass, types: Array[AType],
     _probes: Iterable[String])(implicit sf: SampleFilter) extends
     t.viewer.server.AssociationResolver(probeStore, b2rKegg, mirnaSources, sc, types, _probes) {

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
    def resolveMiRNA(source: MirnaSource, probes: Iterable[Probe],
                     fromMirna: Boolean): MMap[Probe, DefaultBio] = {
      val species = asSpecies(sc)
      val sizeLimit = Some(1000)
      
      try {
        source.id match {
          case "http://level-five.jp/t/mapping/mirdb" =>
          mirnaTable match {
            case None =>
            println("Warning: no mirnaTable available, lookups will fail")
            emptyMMap()
            case Some(t) =>
            //TODO should perform this filtering once and store in matrix state
            val filtTable = t.speciesFilter(species)
            println(s"Lookup from miRNA table of size ${filtTable.size}")
            //TODO unify this lookup with the "aprobes" mechanism
            val lookedUp = platforms.resolve(probes.map(_.identifier).toSeq)
            //TODO filter the platforms properly, select the right one
            val data = t.associationLookup(lookedUp, fromMirna,
              probeStore.platformsAndProbes.flatMap(_._2), sizeLimit)
            if (Some(data.size) == sizeLimit) {
              sizeLimitExceeded = true
            }
            data
          }

          //TODO handle reverse lookup case here
          case AppInfoLoader.TARGETMINE_SOURCE =>
            toBioMap(probes, (_: Probe).genes) combine
            mirnaResolver.forGenes(probes.flatMap(_.genes))

          case _ => throw new Exception(s"Unexpected miRNA source ${source.id}")
        }

      } catch {
        case e: Exception =>
        e.printStackTrace()
        emptyMMap()
      }
    }

  def resolveMiRNA(probes: Iterable[Probe], fromMirna: Boolean): BBMap = {
    val (isMirna, isNotMirna) = probes.partition(_.isMiRna)

    if (!fromMirna) {
      val immediateLookup = probeStore.mirnaAccessionLookup(isMirna)

      val empty = emptyMMap[Probe, DefaultBio]()
      val resolved = mirnaSources.par.map(s =>
        resolveMiRNA(s, isNotMirna, fromMirna)).seq.foldLeft(empty)(_ union _)

      immediateLookup ++ resolved
    } else {
      val empty = emptyMMap[Probe, DefaultBio]()
      mirnaSources.par.map(s =>
        resolveMiRNA(s, isMirna, fromMirna)).seq.foldLeft(empty)(_ union _)
    }
  }

    lazy val mirnaResolver = TargetmineColumns.miRNA(targetmine.get)

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
