package otgviewer.server

import t.platform.Species._
import otg.sparql.OTGSamples
import otg.sparql.Probes
import t.common.shared.AType
import t.model.SampleClass
import t.platform.Probe
import t.sparql._
import t.sparql.secondary._
import t.sparql.toBioMap
import t.viewer.server.intermine.IntermineConnector
import t.viewer.server.intermine.TargetmineColumns
import t.viewer.shared.mirna.MirnaSource

/**
 * Association resolver for Open TG-GATEs-specific associations.
 */
class AssociationResolver(probeStore: Probes,
    sampleStore: OTGSamples,
    b2rKegg: B2RKegg,
    uniprot: Uniprot,
    chembl: ChEMBL,
    drugBank: DrugBank,    
    targetmine: Option[IntermineConnector],
    mirnaSources: Seq[MirnaSource],
    sc: SampleClass, types: Array[AType],
     _probes: Iterable[String]) extends
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
      /*
       * To do here: 1. Make lookups parallel
       * 2. Respect filters and chosen sources, if any
       */
      
      val (isMirna, isNotMirna) = probes.partition(_.isMiRna)
      val immediateLookup = probeStore.mirnaAccessionLookup(isMirna)

      val sourceMap = Map() ++ mirnaSources.map(x => x.id() -> x)
      
      val viaGeneAnnotation = toBioMap(isNotMirna, (_: Probe).genes) combine
          mirnaResolver.forGenes(probes.flatMap(_.genes))

      val viaSparql = probeStore.mirnaAssociations(probes, sourceMap.get("http://level-five.jp/t/mapping/mirdb").map(_.limit()))

      immediateLookup ++ viaGeneAnnotation ++ viaSparql
    }

    lazy val mirnaResolver = TargetmineColumns.miRNA(targetmine.get)

    override def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe])(implicit sf: SampleFilter): BBMap = {
      at match {
        case _: AType.GOMF.type       => probeStore.mfGoTerms(probes)
        case _: AType.GOBP.type       => probeStore.bpGoTerms(probes)
        case _: AType.GOCC.type       => probeStore.ccGoTerms(probes)
        case _: AType.OrthProts.type  => oproteins
        case _: AType.Chembl.type     => getTargeting(sc, chembl)
        case _: AType.Drugbank.type   => getTargeting(sc, drugBank)
        case _: AType.RefseqTrn.type  => probeStore.refseqTrnLookup(probes)
        case _: AType.RefseqProt.type => probeStore.refseqProtLookup(probes)
        case _: AType.Ensembl.type    => probeStore.ensemblLookup(probes)
        case _: AType.EC.type         => probeStore.ecLookup(probes)
        case _: AType.Unigene.type    => probeStore.unigeneLookup(probes)
        case _: AType.MiRNA.type      => resolveMiRNA(probes)
        case _                        => super.associationLookup(at, sc, probes)
      }
    }
  }
