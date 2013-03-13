package otgviewer.server

import com.google.gwt.user.server.rpc.RemoteServiceServlet

import Assocations.convert
import UtilsS.nullToNone
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import kyotocabinet.DB
import otg.OTGQueries
import otg.OTGSeriesQuery
import otg.Series
import otg.Species
import otg.sparql._
import otgviewer.client.SparqlService
import otgviewer.shared.AType
import otgviewer.shared.Annotation
import otgviewer.shared.Association
import otgviewer.shared.Barcode
import otgviewer.shared.DataColumn
import otgviewer.shared.DataFilter
import otgviewer.shared.MatchResult
import otgviewer.shared.NoSuchProbeException
import otgviewer.shared.Pair
import otgviewer.shared.Pathology
import otgviewer.shared.RankRule

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class SparqlServiceImpl extends RemoteServiceServlet with SparqlService {
  import Conversions._
  import UtilsS._
  import Assocations._
  import CommonSPARQL._
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit()
  }
  
  def localInit() {
    OTGSamples.connect()
    AffyProbes.connect()
    Uniprot.connect()    
  }

  override def destroy() {
    AffyProbes.close()
    OTGSamples.close()
    Uniprot.close()   
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
    
//  def probeTitle(probe: String): String = 
//    AffyProbes.title(probe)
//    
  def probes(filter: DataFilter): Array[String] = 
    OTGQueries.probeIds(filter).toArray
    
  def pathologies(barcode: Barcode): Array[Pathology] = 
    OTGSamples.pathologies(barcode.getCode).map(asJava(_))
    
  def pathologies(column: DataColumn): Array[Pathology] = 
    column.getBarcodes.flatMap(x => OTGSamples.pathologies(x.getCode)).map(asJava(_))
    
  def annotations(barcode: Barcode): Annotation = asJava(OTGSamples.annotations(barcode.getCode))
  def annotations(column: DataColumn): Array[Annotation] = 
    column.getBarcodes.map(x => OTGSamples.annotations(x.getCode)).map(asJava(_))
    
  def pathways(filter: DataFilter, pattern: String): Array[String] = 
    useConnector(B2RKegg, (c: B2RKegg.type) => c.pathways(pattern, filter)).toArray    
  
    //TODO: return a map instead
  def geneSyms(probes: Array[String], filter: DataFilter): Array[Array[String]] = {
    val ps = probes.map(p => Probe(p))
    val attrib = AffyProbes.withAttributes(ps, filter)
    attrib.toArray.map(_.symbols.toArray.map(_.symbol))    
  }
    
  def probesForPathway(filter: DataFilter, pathway: String): Array[String] = {
    useConnector(B2RKegg, (c: B2RKegg.type) => {
      val geneIds = c.geneIds(pathway, filter).map(Gene(_))
      println("Probes for " + geneIds.length + " genes")
      val probes = AffyProbes.forGenes(geneIds).toArray 
      OTGQueries.filterProbes(probes.map(_.identifier), filter).toArray  
    })    
  }
  def probesTargetedByCompound(filter: DataFilter, compound: String, service: String, homologous: Boolean): Array[String] = {
    val cmp = Compound.make(compound)
    val proteins = service match {
      case "CHEMBL" => useConnector(ChEMBL, (c:ChEMBL.type) => c.targetsFor(cmp, 
          if (homologous) { null } else { filter }))              
      case "DrugBank" => useConnector(DrugBank, (c:DrugBank.type) => c.targetsFor(cmp))        
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
    val pbs = if (homologous) {
      val oproteins = Uniprot.orthologsForUniprots(proteins).values.flatten
      AffyProbes.forUniprots(oproteins)
//      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      AffyProbes.forUniprots(proteins)
    }
    pbs.filter(p => OTGQueries.isProbeForSpecies(p.identifier, filter)).map(_.identifier).toArray  
  }
  
  def goTerms(pattern: String): Array[String] = 
    OTGSamples.goTerms(pattern)
    
  def probesForGoTerm(filter: DataFilter, goTerm: String): Array[String] = 
    OTGQueries.filterProbes(
        AffyProbes.forGoTerm(GOTerm("", goTerm)).map(_.identifier).toSeq, 
        filter).toArray

//    import AffyProbes._

    import scala.collection.{Map => CMap, Set => CSet}
    
  def associations(filter: DataFilter, types: Array[AType], _probes: Array[String]): Array[Association] = {
    val probes = AffyProbes.withAttributes(_probes.map(Probe(_)), filter)    
    
    def connectorOrEmpty[T <: RDFConnector](c: T, f: T => BBMap): BBMap = 
      useConnector(c, f, 
          Map() ++ probes.map(p => (Probe(p.identifier) -> CSet(DefaultBio("(Timeout or error)", null)))))

    val proteins: MMap[Probe, Protein] = toBioMap(probes, (p: Probe) => p.proteins) 
 
    val oproteins = if (types.contains(AType.Chembl) || types.contains(AType.Drugbank) ||
        types.contains(AType.KOProts)) {    	
      composeWith(proteins, 
          (ps: Iterable[Protein]) => Uniprot.orthologsForUniprots(ps))
    } else {
      emptyMMap[Probe, Protein]()
    }
    
    def standardMapping(m: BBMap): MMap[String, (String, String)] = 
      m.mapKValues(_.identifier).mapMValues(p => (p.name, p.identifier))
    
    import Association._
    def lookupFunction(t: AType): BBMap = t match {
      case x: AType.Chembl.type => connectorOrEmpty(ChEMBL,
            (c: ChEMBL.type) => {
              val expected = OTGSamples.compounds(filter).map(Compound.make(_))

              //strictly orthologous proteins
              val oproteinVs = oproteins.allValues.toSet -- proteins.allValues.toSet               
              val allProteins = union(proteins, oproteins)              
              val allTargets = c.targetingFor(allProteins.allValues, null, expected)
              
              composeBioMaps(allProteins, allTargets.map(x => if (oproteinVs.contains(x._1)) {
                (x._1 -> x._2.map(c => c.copy(name = c.name + " (inf)"))) 
              } else {
                x 
              }))                              
            })      
      case x: AType.Drugbank.type => connectorOrEmpty(DrugBank,
            (c: DrugBank.type) => {
              val expected = OTGSamples.compounds(filter).map(Compound.make(_))
              val oproteinVs = oproteins.allValues.toSet -- proteins.allValues.toSet               
              val allProteins = union(proteins, oproteins)              
              val allTargets = c.targetingFor(allProteins.allValues, expected)
              
               composeBioMaps(allProteins, allTargets.map(x => if (oproteinVs.contains(x._1)) {
                (x._1 -> x._2.map(c => c.copy(name = c.name + " (inf)"))) 
              } else {
                x 
              })) 
            })
      case x: AType.Uniprot.type => proteins
      case x: AType.KOProts.type => oproteins
      case x: AType.GOMF.type => connectorOrEmpty(AffyProbes,            
            (c: AffyProbes.type) => c.mfGoTerms(probes))         
      case x: AType.GOBP.type => connectorOrEmpty(AffyProbes,            
            (c: AffyProbes.type) => c.bpGoTerms(probes))
      case x: AType.GOCC.type => connectorOrEmpty(AffyProbes,            
            (c: AffyProbes.type) => c.ccGoTerms(probes))
      case x: AType.Homologene.type => connectorOrEmpty(B2RHomologene,
            (c: B2RHomologene.type) => composeBioMaps(probes, (p: Probe) => p.genes, 
                c.homologousGenes(probes.flatMap(_.genes))))    
      case _ => emptyMMap[DefaultBio, DefaultBio]()
//      case x: AType.KEGG.type => connectorOrEmpty(B2RKegg,
//            (c: B2RKegg.type) => unpackBio(c.pathways(probes, filter)))
            
    }
      
    types.par.map(x => (x,standardMapping(lookupFunction(x)))).seq.map(p => new Association(p._1, 
        convertPairs(p._2))).toArray     
  }
  
  def geneSuggestions(partialName: String, filter: DataFilter): Array[Pair[String, String]] = {
    useConnector(AffyProbes, (c: AffyProbes.type) => c.probesForPartialTitle(partialName, filter)).map(x => 
      new Pair(x.identifier, x.name)).toArray
  }
  
}