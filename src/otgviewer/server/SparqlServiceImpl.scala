package otgviewer.server

import com.google.gwt.user.server.rpc.RemoteServiceServlet

import Assocations.convert
import UtilsS.nullToNone
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import kyotocabinet.DB
import otg.OTGMisc
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
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    OTGOwlim.connect()
    B2RAffy.connect()
    Uniprot.connect()
    val homePath = System.getProperty("otg.home")    
  }

  override def destroy() {
    B2RAffy.close()
    OTGOwlim.close()
    Uniprot.close()   
    super.destroy()
  }

  def compounds(filter: DataFilter): Array[String] = 
    OTGOwlim.compounds(filter).toArray
    

  def organs(filter: DataFilter, compound: String): Array[String] = 
    OTGOwlim.organs(filter, nullToOption(compound)).toArray
    
  val orderedDoses = List("Control", "Low", "Middle", "High")
  def doseLevels(filter: DataFilter, compound: String): Array[String] = { 
    val r = OTGOwlim.doseLevels(filter, nullToOption(compound)).toArray
    r.sortWith((d1, d2) => orderedDoses.indexOf(d1) < orderedDoses.indexOf(d2))
  }
  
  def barcodes(filter: DataFilter, compound: String, doseLevel: String, time: String) =
    OTGOwlim.barcodes(filter, nullToNone(compound), 
        nullToNone(doseLevel), nullToNone(time)).map(asJava(_)).toArray
  
  def barcodes(filter: DataFilter, compounds: Array[String], doseLevel: String, time: String) =
    OTGOwlim.barcodes(filter, compounds, 
        nullToNone(doseLevel), nullToNone(time)).map(asJava(_)).toArray
    
        
  val orderedTimes = List("2 hr", "3 hr", "6 hr", "8 hr", "9 hr", "24 hr", "4 day", "8 day", "15 day", "29 day")
  def times(filter: DataFilter, compound: String): Array[String] = { 
    val r = OTGOwlim.times(filter, nullToOption(compound)).toArray    
    r.sortWith((t1, t2) => orderedTimes.indexOf(t1) < orderedTimes.indexOf(t2))
  }
    
  def probeTitle(probe: String): String = 
    B2RAffy.title(probe)
    
  def probes(filter: DataFilter): Array[String] = 
    OTGQueries.probeIds(filter)
    
  def pathologies(barcode: Barcode): Array[Pathology] = 
    OTGOwlim.pathologies(barcode.getCode).map(asJava(_))
    
  def pathologies(column: DataColumn): Array[Pathology] = 
    column.getBarcodes.flatMap(x => OTGOwlim.pathologies(x.getCode)).map(asJava(_))
    
  def annotations(barcode: Barcode): Annotation = asJava(OTGOwlim.annotations(barcode.getCode))
  def annotations(column: DataColumn): Array[Annotation] = 
    column.getBarcodes.map(x => OTGOwlim.annotations(x.getCode)).map(asJava(_))
    
  def pathways(filter: DataFilter, pattern: String): Array[String] = 
    useConnector(B2RKegg, (c: B2RKegg.type) => c.pathways(pattern, filter)).toArray    
  
  def geneSyms(probes: Array[String]): Array[Array[String]] = 
    B2RAffy.geneSyms(probes).map(_.toArray).toArray
    
  def probesForPathway(filter: DataFilter, pathway: String): Array[String] = {
    useConnector(B2RKegg, (c: B2RKegg.type) => {
      val geneIds = c.geneIds(pathway, filter)
      println("Probes for " + geneIds.length + " genes")
      val probes = OTGOwlim.probesForEntrezGenes(geneIds).toArray 
      OTGQueries.filterProbes(probes, filter)
    })    
  }
  def probesTargetedByCompound(filter: DataFilter, compound: String, service: String, homologous: Boolean): Array[String] = {
    val proteins = (service match {
      case "CHEMBL" => useConnector(CHEMBL, (c:CHEMBL.type) => c.targetProtsForCompound(compound, 
          if (homologous) { null } else { filter }))              
      case "DrugBank" => useConnector(DrugBank, (c:DrugBank.type) => c.targetProtsForDrug(compound))        
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    })
    val pbs = if (homologous) {
      val oproteins = Uniprot.orthologsForUniprots(proteins).values.flatten
      OTGOwlim.probesForUniprot(oproteins)
//      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      OTGOwlim.probesForUniprot(proteins)
    }
    OTGQueries.filterProbes(pbs.toArray, filter)
  }
  
  def goTerms(pattern: String): Array[String] = 
    OTGOwlim.goTerms(pattern)
    
  def probesForGoTerm(filter: DataFilter, goTerm: String): Array[String] = 
    OTGQueries.filterProbes(OTGOwlim.probesForGoTerm(goTerm), filter)

    import OTGMisc._

    import scala.collection.{Map => CMap, Set => CSet}
    
  def associations(filter: DataFilter, types: Array[AType], probes: Array[String], geneIds: Array[String]): Array[Association] = {
    def valNames(m: SMPMap): Set[String] = m.flatMap(_._2).map(_._1).toSet
    
    def connectorOrEmpty[T <: RDFConnector](c: T, f: T => SMPMap): SMPMap = 
      useConnector(c, f, Map() ++ probes.map(p => (p -> CSet(("(Timeout or error)", null: String)))))

    //In the case where m1 maps T->U and m2 maps U->V, produce a map T->V.
    def composeMaps(m1: SMPMap, m2: SMPMap): SMPMap = 
      m1.map(x => (x._1 -> x._2.flatMap(titleAndId => m2.getOrElse(titleAndId._1, Set()))))
      
    //compose by applying a lookup function to U from the first map T->U
    def composeWith(m1: SMPMap, lookup: (Iterable[String]) => SMPMap): SMPMap = 
      composeMaps(m1, lookup(valNames(m1)))
    
    def union(m1: SMPMap, m2: SMPMap): SMPMap = {
      val allKeys = m1.keySet ++ m2.keySet
      Map() ++ allKeys.map(k => k -> (m1.getOrElse(k, Set()) ++ m2.getOrElse(k, Set())))
    } 
    
    def double(m: SMMap): SMPMap = m.map(x => (x._1 -> x._2.map(y => (y, y))))
    
    //this should be done in a separate future, kind of
    val proteins = if (types.contains(AType.Chembl) || types.contains(AType.Drugbank) ||
        types.contains(AType.KOProts) || types.contains(AType.Uniprot))  {      
    	double(OTGOwlim.uniprotsForProbes(probes))
    } else {
      emptySMPMap
    }
    
    val oproteins = if (types.contains(AType.Chembl) || types.contains(AType.Drugbank) ||
        types.contains(AType.KOProts)) {
      composeWith(proteins, ps => double(Uniprot.orthologsForUniprots(ps)))
    } else {
      emptySMPMap
    }
    
    import Association._
    def lookupFunction(t: AType) = t match {
      case x: AType.Chembl.type => connectorOrEmpty(CHEMBL,
            (c: CHEMBL.type) => {
              val compounds = OTGOwlim.compounds(filter)

              //strictly orthologous proteins
              val oproteinVs = valNames(oproteins) -- valNames(proteins)              
              val allProteins = union(proteins, oproteins)              
              val allTargets = c.targetingCompoundsForProteins(valNames(allProteins), null, compounds)
              
              composeMaps(allProteins, allTargets.map(x => (x._1 -> x._2.map(c => 
                if (oproteinVs.contains(x._1)) { (c._1 + "(inf)", c._2) } else { c }))))              
            })      
      case x: AType.Drugbank.type => connectorOrEmpty(DrugBank,
            (c: DrugBank.type) => {
              val compounds = OTGOwlim.compounds(filter)
              val oproteinVs = valNames(oproteins) -- valNames(proteins)               
              val allProteins = union(proteins, oproteins)              
              val allTargets = c.targetingCompoundsForProteins(valNames(allProteins), compounds)
              composeMaps(allProteins, allTargets.map(x => (x._1 -> x._2.map(c => 
                if (oproteinVs.contains(x._1)) { (c._1 + "(inf)", c._2) } else { c }))))
            })
      case x: AType.Uniprot.type => proteins
      case x: AType.KOProts.type => oproteins
      case x: AType.GOMF.type => connectorOrEmpty(OTGOwlim,            
            (c: OTGOwlim.type) => c.mfGoTermsForProbes(probes))         
      case x: AType.GOBP.type => connectorOrEmpty(OTGOwlim,            
            (c: OTGOwlim.type) => c.bpGoTermsForProbes(probes))        
      case x: AType.GOCC.type => connectorOrEmpty(OTGOwlim,            
            (c: OTGOwlim.type) => c.ccGoTermsForProbes(probes))                  
      case x: AType.Homologene.type => connectorOrEmpty(B2RHomologene,
            (c: B2RHomologene.type) => double(c.homologousGenes(geneIds)))    
      case x: AType.KEGG.type => connectorOrEmpty(B2RKegg,
            (c: B2RKegg.type) => c.pathwaysForProbes(probes, filter))
            
    }
      
    types.par.map(x => (x,lookupFunction(x))).seq.map(p => new Association(p._1, 
        convertPairs(p._2))).toArray     
  }
  
  def geneSuggestions(partialName: String, filter: DataFilter): Array[Pair[String, String]] = {
    useConnector(B2RAffy, (c: B2RAffy.type) => c.probesForPartialTitle(partialName, filter)).map(x => 
      new Pair(x._1, x._2)).toArray
  }
  
}