package otgviewer.server.rpc

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asJavaMap
import scala.collection.{Set => CSet}
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Conversions.asJava
import Conversions.asJavaSample
import Conversions.convertPairs
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.OTGBConfig
import otg.OTGContext
import otg.Species.Human
import t.sparql.secondary._
import otg.sparql._
import t.sparql._
import otgviewer.server.ScalaUtils.gracefully
import otgviewer.shared.OTGColumn
import otgviewer.shared.OTGSample
import otgviewer.shared.Pathology
import t.BaseConfig
import t.DataConfig
import t.TriplestoreConfig
import t.common.shared.SampleClass
import t.common.shared.sample.Annotation
import t.common.shared.sample.HasSamples
import t.common.shared.Pair
import t.db.DefaultBio
import t.sparql.Instances
import t.sparql.Triplestore
import t.sparql.TriplestoreMetadata
import t.viewer.server.Configuration
import t.viewer.server.Conversions.asSpecies
import t.viewer.server.Conversions.scAsScala
import t.viewer.shared.Association
import otgviewer.server.ScalaUtils
import otgviewer.shared.TimeoutException
import otgviewer.shared.OTGSchema
import t.platform.Probe
import otg.sparql.Probes
import t.common.shared.Dataset
import otgviewer.shared.Group
import t.common.shared.AType


/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class SparqlServiceImpl extends t.common.server.rpc.SparqlServiceImpl with OTGServiceServlet {
  
  private def probeStore: otg.sparql.Probes = context.probes
  private def sampleStore: otg.sparql.OTGSamples = context.samples

  var chembl: ChEMBL = _
  var drugBank: DrugBank = _
  var homologene: B2RHomologene = _
  
  override def localInit(c: Configuration) {
    super.localInit(c)
    chembl = new ChEMBL()
    drugBank = new DrugBank()
    homologene = new B2RHomologene()
    
    _appInfo.setPredefinedGroups(predefinedGroups)
  }
  
  @throws[TimeoutException]
  override def goTerms(pattern: String): Array[String] =
    probeStore.goTerms(pattern).map(_.name).toArray

  @throws[TimeoutException]
  override def probesForGoTerm(goTerm: String): Array[String] = {
    val pmap = context.matrix.probeMap 
    probeStore.forGoTerm(GOTerm("", goTerm)).map(_.identifier).filter(pmap.isToken).toArray
  }
  
  //TODO move to OTG
  @throws[TimeoutException]
  private def predefinedGroups: Array[Group] = {
    //we call this from localInit and sessionInfo.sampleFilter
    //will not be available yet
    
    implicit val sf = SampleFilter(instanceURI = instanceURI)
    val r = sampleStore.sampleGroups.filter(!_._2.isEmpty).map(x => 
      new Group(schema, x._1, x._2.map(x => asJavaSample(x)).toArray))
    r.toArray
  }

  //TODO move to OTG
  @throws[TimeoutException]
  override def probesTargetedByCompound(sc: SampleClass, compound: String, service: String,
    homologous: Boolean): Array[String] = {
    val cmp = Compound.make(compound)
    val sp = asSpecies(sc)
    val proteins = service match {
      case "CHEMBL" => chembl.targetsFor(cmp)
      case "DrugBank" => drugBank.targetsFor(cmp)
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
    val pbs = if (homologous) {
      val oproteins = uniprot.orthologsFor(proteins, sp).values.flatten.toSet
      probeStore.forUniprots(oproteins ++ proteins)
      //      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      probeStore.forUniprots(proteins)
    }
    val pmap = context.matrix.probeMap //TODO context.probes(filter)
    pbs.toSet.map((p: Probe) => p.identifier).filter(pmap.isToken).toArray
  }

  @throws[TimeoutException]
  override def pathologies(column: OTGColumn): Array[Pathology] =
    column.getSamples.flatMap(x => sampleStore.pathologies(x.getCode)).map(asJava(_))
  
  override def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] =    
    new AnnotationResolver(sc, types, _probes).resolve

  protected class AnnotationResolver(sc: SampleClass, types: Array[AType],
     _probes: Iterable[String]) extends super.AnnotationResolver(sc, types, _probes) {

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
      val expected = sampleStore.compounds(scAsScala(sc).filterAll).map(Compound.make(_))

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
       
    override def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe]): BBMap = {
      at match {
        case _: AType.GOMF.type      => probeStore.mfGoTerms(probes)
        case _: AType.GOBP.type      => probeStore.bpGoTerms(probes)
        case _: AType.GOCC.type      => probeStore.ccGoTerms(probes)
        case _: AType.OrthProts.type => oproteins
        case _: AType.Chembl.type    => getTargeting(sc, chembl)
        case _: AType.Drugbank.type  => getTargeting(sc, drugBank)
        case _                       => super.associationLookup(at, sc, probes)
      }
    }
  }
}