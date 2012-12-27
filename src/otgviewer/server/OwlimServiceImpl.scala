package otgviewer.server

import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Assocations.convert
import UtilsS.nullToNone
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.B2RAffy
import otg.B2RKegg
import otg.B2RKegg
import otg.CHEMBL
import otg.CHEMBL
import otg.DrugBank
import otg.DrugBank
import otg.OTGOwlim
import otg.OTGQueries
import otgviewer.client.OwlimService
import otgviewer.shared.Annotation
import otgviewer.shared.Association
import otgviewer.shared.Barcode
import otgviewer.shared.DataColumn
import otgviewer.shared.DataFilter
import otgviewer.shared.Pathology
import otgviewer.shared.Pair
import otgviewer.shared.RankRule
import otg.OTGSeriesQuery
import kyotocabinet.DB
import otg.Series
import otg.OTGMisc
import otgviewer.shared.NoSuchProbeException

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class OwlimServiceImpl extends RemoteServiceServlet with OwlimService {
  import Conversions._
  import UtilsS._
  import Assocations._
  
  private var seriesDB: DB = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    OTGOwlim.connect()
    B2RAffy.connect()
    val homePath = System.getProperty("otg.home")
    seriesDB = OTGSeriesQuery.open(homePath + "/otgfs.kct")
  }

  override def destroy() {
    B2RAffy.close()
    OTGOwlim.close()
    seriesDB.close()
    super.destroy()
  }

  def compounds(filter: DataFilter): Array[String] = 
    OTGOwlim.compounds(filter).toArray
    
  import java.lang.{Double => JDouble}
  def rankedCompounds(filter: DataFilter, rules: Array[RankRule]): Array[Pair[String, JDouble]] = {
    val nnr = rules.takeWhile(_ != null)
    var srs = nnr.map(asScala(_))    
    var probesRules = nnr.map(_.probe).zip(srs)
    
    //Convert the input probes (which may actually be genes) into definite probes
    probesRules = probesRules.flatMap(pr => {
      val resolved = OTGMisc.identifiersToProbesQuick(filter, Array(pr._1), true)
      if (resolved.length == 0) {
        throw new NoSuchProbeException(pr._1)
      }
      resolved.map(r => (r, pr._2))
    })
    
    //TODO: probe is actually irrelevant here but the API is not well designed
    //Same for timeDose = High
    val key = asScala(filter, new otgviewer.shared.Series("", probesRules.head._1, "High", null, Array.empty)) 
    
    val r = OTGSeriesQuery.rankCompoundsCombined(seriesDB, key, probesRules).map(p => asJava[String, JDouble](p._1, p._2.toDouble)).toArray
    val rr = r.sortWith((x1, x2) => {
      if (JDouble.isNaN(x1.second)) {
        false
      } else if (JDouble.isNaN(x2.second)) {
        true
      } else {
        x1.second > x2.second
      }
    })

    for (s <- rr.take(10)) {
      println(s)
    }
    rr
  }
  
  def organs(filter: DataFilter, compound: String): Array[String] = 
    OTGOwlim.organs(filter, nullToOption(compound)).toArray
    
  val orderedDoses = List("Control", "Low", "Middle", "High")
  def doseLevels(filter: DataFilter, compound: String): Array[String] = { 
    val r = OTGOwlim.doseLevels(filter, nullToOption(compound)).toArray
    r.sortWith((d1, d2) => orderedDoses.indexOf(d1) < orderedDoses.indexOf(d2))
  }
  
  def barcodes(filter: DataFilter, compound: String, doseLevel: String, time: String) =
    OTGOwlim.barcodes(filter, nullToNone(compound), 
        nullToNone(doseLevel), nullToNone(time)).map(asJava(_, compound)).toArray
    
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
    useConnector(B2RKegg, (c: B2RKegg.type) => c.pathways(pattern, filter))    
  
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
  def probesTargetedByCompound(filter: DataFilter, compound: String, service: String): Array[String] = {
    val proteins = (service match {
      case "CHEMBL" => useConnector(CHEMBL, (c:CHEMBL.type) => c.targetProtsForCompound(compound, filter))              
      case "DrugBank" => useConnector(DrugBank, (c:DrugBank.type) => c.targetProtsForDrug(compound))        
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    })
    val pbs = OTGOwlim.probesForUniprot(proteins).toArray
    OTGQueries.filterProbes(pbs, filter)
  }
  
  def goTerms(pattern: String): Array[String] = 
    OTGOwlim.goTerms(pattern)
    
  def probesForGoTerm(filter: DataFilter, goTerm: String): Array[String] = 
    OTGQueries.filterProbes(OTGOwlim.probesForGoTerm(goTerm), filter)

  def associations(filter: DataFilter, probes: Array[String]): Array[Association] = {

    //By setting it up like this we can request data from the different servers in parallel,
    //and keep going if one fails
    val sources = List(() => {
      ("KEGG pathways", convert(
          useConnector(B2RKegg,
              (c: B2RKegg.type) => c.pathwaysForProbes(probes, filter),
              Map())))                
    },
      () => {
        ("MF GO terms",
          convert(useConnector(OTGOwlim,            
            (c: OTGOwlim.type) => c.mfGoTermsForProbes(probes),
            Map())))          
      },
      () => {
        ("BP GO terms",
          convert(useConnector(OTGOwlim,            
            (c: OTGOwlim.type) => c.bpGoTermsForProbes(probes),
            Map())))          
      },
      () => {
        ("CC GO terms",
          convert(useConnector(OTGOwlim,            
            (c: OTGOwlim.type) => c.ccGoTermsForProbes(probes),
            Map())))          
      }
      )

    sources.par.map(_()).seq.map(x => new Association(x._1, x._2)).toArray    
  }
  
  def geneSuggestions(partialName: String): Array[Pair[String, String]] = {
    useConnector(B2RAffy, (c: B2RAffy.type) => c.probesForPartialTitle(partialName, null)).map(x => 
      new Pair(x._1, x._2)).toArray
  }
  
}