package otgviewer.server

import com.google.gwt.user.server.rpc.RemoteServiceServlet
import otgviewer.client.OwlimService
import otg.OTGOwlim
import otg.B2RAffy
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otgviewer.shared.DataFilter
import otg.OTGQueries
import otgviewer.shared.Pathology
import otgviewer.shared.Barcode
import otgviewer.shared.DataColumn
import otgviewer.shared.Association
import otg.B2RKegg
import otg.CHEMBL
import otg.DrugBank
import otg.CHEMBL
import otgviewer.shared.Annotation

/**
 * This servlet is reponsible for making queries to RDF stores, including our
 * local Owlim-lite store.
 */
class OwlimServiceImpl extends RemoteServiceServlet with OwlimService {
  import Conversions._
  import UtilsS._
  import Assocations._

  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    OTGOwlim.connect()
    B2RAffy.connect()
  }

  override def destroy() {
    B2RAffy.close()
    OTGOwlim.close()
    super.destroy()
  }

  def compounds(filter: DataFilter): Array[String] = 
    OTGOwlim.compounds(filter)
    
  def organs(filter: DataFilter, compound: String): Array[String] = 
    OTGOwlim.organs(filter, compound)
    
  def doseLevels(filter: DataFilter, compound: String) = 
    OTGOwlim.doseLevels(filter, compound)
  
  def barcodes(filter: DataFilter, compound: String, doseLevel: String, time: String) =
    OTGOwlim.barcodes(filter, nullToNone(compound), 
        nullToNone(doseLevel), nullToNone(time)).map(asJava(_, compound))
    
  def times(filter: DataFilter, compound: String): Array[String] = 
    OTGOwlim.times(filter, compound)
    
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
    B2RAffy.geneSyms(probes)
    
  def probesForPathway(filter: DataFilter, pathway: String): Array[String] = {
    useConnector(B2RKegg, (c: B2RKegg.type) => {
      val geneIds = c.geneIds(pathway, filter)
      println("Probes for " + geneIds.length + " genes")
      val probes = OTGOwlim.probesForEntrezGenes(geneIds) 
      OTGQueries.filterProbes(probes, filter)
    })    
  }
  def probesTargetedByCompound(filter: DataFilter, compound: String, service: String): Array[String] = {
    service match {
      case "CHEMBL" => useConnector(CHEMBL, (c:CHEMBL.type) => c.targetProtsForCompound(compound, filter))              
      case "DrugBank" => useConnector(DrugBank, (c:DrugBank.type) => c.targetProtsForDrug(compound))
        
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
  }
  
  def goTerms(pattern: String): Array[String] = 
    OTGOwlim.goTerms(pattern)
    
  def probesForGoTerm(filter: DataFilter, goTerm: String): Array[String] = 
    OTGQueries.filterProbes(OTGOwlim.probesForGoTerm(goTerm), filter)
    
  def associations(filter: DataFilter, probes: Array[String]): Array[Association] = {

    try {
      B2RKegg.connect()
      OTGOwlim.connect()

      val sources = List(() => ("KEGG pathways", convert(B2RKegg.pathwaysForProbes(probes, filter))),
        () => ("GO terms", getGoterms(probes)))

      sources.par.map(_()).seq.map(x => new Association(x._1, x._2)).toArray
    } finally {
      B2RKegg.close()
      OTGOwlim.close()
    }
  }
  
}