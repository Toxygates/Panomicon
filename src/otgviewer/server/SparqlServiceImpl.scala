package otgviewer.server

import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom
import scala.Option.option2Iterable
import scala.collection.{Set => CSet}
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Assocations.convertPairs
import Conversions.asJava
import Conversions.asScala
import Conversions.nullToOption
import Conversions.speciesFromFilter
import UtilsS.nullToNone
import UtilsS.useConnector
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.DefaultBio
import otg.OTGQueries
import otg.sparql._
import otgviewer.client.SparqlService
import otgviewer.shared.AType
import otgviewer.shared.Association
import otgviewer.shared.Barcode
import otgviewer.shared.DataFilter
import otgviewer.shared.BarcodeColumn
import bioweb.shared.Pair
import otgviewer.shared.Pathology
import bioweb.shared.array.Annotation
import otg.Human

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class SparqlServiceImpl extends RemoteServiceServlet with SparqlService {
  import Conversions._
  import UtilsS._
  import Assocations._
  import CommonSPARQL._
  
  type DataColumn = bioweb.shared.array.DataColumn[Barcode]
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }
  
  def localInit(conf: Configuration) {
    otg.Configuration.owlimRepositoryName = conf.owlimRepositoryName    
    OTGSamples.connect()
    AffyProbes.connect()
    LocalUniprot.connect()    
  }
  
  override def destroy() {
    AffyProbes.close()
    OTGSamples.close()
    LocalUniprot.close()   
    super.destroy()
  }

  def compounds(filter: DataFilter): Array[String] = 
    OTGSamples.compounds(filter).toArray

  def organs(filter: DataFilter, compound: String): Array[String] = 
    OTGSamples.organs(filter, nullToOption(compound)).toArray
    
  val orderedDoses = List("Control", "Low", "Middle", "High")
  def doseLevels(filter: DataFilter, compound: String): Array[String] = { 
    val r = OTGSamples.doseLevels(filter, nullToOption(compound)).toArray
    r.sortWith((d1, d2) => orderedDoses.indexOf(d1) < orderedDoses.indexOf(d2))
  }
  
  def barcodes(filter: DataFilter, compound: String, doseLevel: String, time: String) =
    OTGSamples.barcodes(filter, nullToNone(compound), 
        nullToNone(doseLevel), nullToNone(time)).map(asJava(_)).toArray
  
  def barcodes(filter: DataFilter, compounds: Array[String], doseLevel: String, time: String) =
    OTGSamples.barcodes(filter, compounds, 
        nullToNone(doseLevel), nullToNone(time)).map(asJava(_)).toArray
    
        
  val orderedTimes = List("2 hr", "3 hr", "6 hr", "8 hr", "9 hr", "24 hr", "4 day", "8 day", "15 day", "29 day")
  def times(filter: DataFilter, compound: String): Array[String] = { 
    val r = OTGSamples.times(filter, nullToOption(compound)).toArray    
    r.sortWith((t1, t2) => orderedTimes.indexOf(t1) < orderedTimes.indexOf(t2))
  }
  
  def timeDoseCombinations(filter: DataFilter, compound: String): Array[Pair[String, String]] = 
    OTGSamples.timeDoseCombinations(filter, compound).map(asJava(_)).toArray
  
  def probes(filter: DataFilter): Array[String] = 
    OTGQueries.probeIds(filter).toArray
    
  def pathologies(barcode: Barcode): Array[Pathology] = 
    OTGSamples.pathologies(barcode.getCode).map(asJava(_)).toArray
    
  def pathologies(column: BarcodeColumn): Array[Pathology] = 
    column.getSamples.flatMap(x => OTGSamples.pathologies(x.getCode)).map(asJava(_))
    
  def annotations(barcode: Barcode): Annotation = asJava(OTGSamples.annotations(barcode.getCode))
  def annotations(column: BarcodeColumn): Array[Annotation] = 
    column.getSamples.map(x => OTGSamples.annotations(x.getCode)).map(asJava(_))
    
  def pathways(filter: DataFilter, pattern: String): Array[String] = 
    useConnector(B2RKegg, (c: B2RKegg.type) => c.forPattern(pattern, filter)).toArray    
  
    //TODO: return a map instead
  def geneSyms(filter: DataFilter, probes: Array[String]): Array[Array[String]] = {
    val ps = probes.map(p => Probe(p))
    val attrib = AffyProbes.withAttributes(ps, filter)
    probes.map(pi => attrib.find(_.identifier == pi).
        map(_.symbolStrings.toArray).getOrElse(Array()))    
  }
    
  def probesForPathway(filter: DataFilter, pathway: String): Array[String] = {
    useConnector(B2RKegg, (c: B2RKegg.type) => {
      val geneIds = c.geneIds(pathway, filter).map(Gene(_))
      println("Probes for " + geneIds.size + " genes")
      val probes = AffyProbes.forGenes(geneIds).toArray 
      OTGQueries.filterProbes(probes.map(_.identifier), filter).toArray  
    })    
  }
  
  def probesTargetedByCompound(filter: DataFilter, compound: String, service: String, 
      homologous: Boolean): Array[String] = {
    val cmp = Compound.make(compound)
    val proteins = service match {
      case "CHEMBL" => useConnector(ChEMBL, (c:ChEMBL.type) => c.targetsFor(cmp, 
          if (homologous) { null } else { filter }))              
      case "DrugBank" => useConnector(DrugBank, (c:DrugBank.type) => c.targetsFor(cmp))        
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
    val pbs = if (homologous) {
      val oproteins = LocalUniprot.orthologsFor(proteins, filter).values.flatten.toSet
      AffyProbes.forUniprots(oproteins ++ proteins)
//      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      AffyProbes.forUniprots(proteins)
    }
    println(pbs.size)
    val f = pbs.filter(p => OTGQueries.isProbeForSpecies(p.identifier, filter)).map(_.identifier).toArray
    println(f.size)
    f
  }
  
  def goTerms(pattern: String): Array[String] = 
    OTGSamples.goTerms(pattern).map(_.name).toArray
    
  def probesForGoTerm(filter: DataFilter, goTerm: String): Array[String] = 
    OTGQueries.filterProbes(
        AffyProbes.forGoTerm(GOTerm("", goTerm)).map(_.identifier).toSeq, 
        filter).toArray

    import scala.collection.{Map => CMap, Set => CSet}
    
  def associations(filter: DataFilter, types: Array[AType], 
      _probes: Array[String]): Array[Association] = {
    val probes = AffyProbes.withAttributes(_probes.map(Probe(_)), filter)    
    
    
    def connectorOrEmpty[T <: RDFConnector](c: T, f: T => BBMap): BBMap = {
      val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
      useConnector(c, f, 
          Map() ++ probes.map(p => (Probe(p.identifier) -> emptyVal)))
    }

    val proteins = toBioMap(probes, (_: Probe).proteins) 
 
    //orthologous proteins if needed
    val oproteins = if (
        (types.contains(AType.Chembl) || types.contains(AType.Drugbank) || types.contains(AType.OrthProts))
        && (filter.species.get != Human)
        && false // Not used currently due to performance issues!
        ) {    	
      // This always maps to Human proteins as they are assumed to contain the most targets
      val r = proteins combine ((ps: Iterable[Protein]) => LocalUniprot.orthologsFor(ps, Human))      
      r
    } else {
      emptyMMap[Probe, Protein]()
    }
    println(oproteins.allValues.size + " oproteins")

    def getTargeting(from: CompoundTargets): MMap[Probe, Compound] = {
      val expected = OTGSamples.compounds(filter).map(Compound.make(_))
      
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
    
    import Association._
    def lookupFunction(t: AType): BBMap = t match {
      case x: AType.Chembl.type => connectorOrEmpty(ChEMBL, getTargeting(_:ChEMBL.type))    
      case x: AType.Drugbank.type => connectorOrEmpty(DrugBank, getTargeting(_:DrugBank.type))              
      case x: AType.Uniprot.type => proteins
      case x: AType.OrthProts.type => oproteins
      case x: AType.GOMF.type => connectorOrEmpty(AffyProbes,            
            (c: AffyProbes.type) => c.mfGoTerms(probes))         
      case x: AType.GOBP.type => connectorOrEmpty(AffyProbes,            
            (c: AffyProbes.type) => c.bpGoTerms(probes))
      case x: AType.GOCC.type => connectorOrEmpty(AffyProbes,            
            (c: AffyProbes.type) => c.ccGoTerms(probes))
      case x: AType.Homologene.type => connectorOrEmpty(B2RHomologene,
            (c: B2RHomologene.type) => toBioMap(probes, (_:Probe).genes) combine 
                c.homologousGenes(probes.flatMap(_.genes)))     
      case x: AType.KEGG.type => connectorOrEmpty(B2RKegg,
            (c: B2RKegg.type) => toBioMap(probes, (_:Probe).genes) combine 
                c.forGenes(probes.flatMap(_.genes), filter))
      case x: AType.Enzymes.type => connectorOrEmpty(B2RKegg,
              (c: B2RKegg.type) => c.enzymes(probes.flatMap(_.genes), filter))
    }
      
        
    def standardMapping(m: BBMap): MMap[String, (String, String)] = 
      m.mapKValues(_.identifier).mapMValues(p => (p.name, p.identifier))
    
    val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)))).seq

    m1.map(p => new Association(p._1, convertPairs(p._2))).toArray     
  }
  
  def geneSuggestions(filter: DataFilter, partialName: String): 
	  Array[bioweb.shared.Pair[String, String]] = {
    useConnector(AffyProbes, 
        (c: AffyProbes.type) => c.probesForPartialTitle(partialName, filter))
        .map(x => new Pair(x.identifier, x.name)).toArray
  }
  
}