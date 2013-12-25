package otgviewer.server.rpc

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import otgviewer.shared.DataFilter
import otgviewer.shared.CellType
import otgviewer.shared.Organ
import otgviewer.shared.Organism
import otgviewer.shared.RepeatType
import otgviewer.shared.AType
import org.scalatest.BeforeAndAfter
import otgviewer.server.rpc.SparqlServiceImpl
import otgviewer.server.Configuration
import org.scalatest.junit.JUnitRunner

object SparqlServiceTest {
  def testFilter = new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat)
}

@RunWith(classOf[JUnitRunner])
class SparqlServiceTest extends FunSuite with BeforeAndAfter {

  var s: SparqlServiceImpl = _
  
  before {    
    val conf = new Configuration("otg", "/ext/toxygates")    
    s = new SparqlServiceImpl()
    s.localInit(conf)
  }  
  
  after {
    s.destroy
  }
  
  val f = SparqlServiceTest.testFilter
  
  val probes = Array("1387936_at", "1391544_at")
//  val geneIds = Array("361510", "362972")
  
  private def testAssociation(typ: AType) = s.associations(f, Array(typ), probes)
    
  test("BP GO terms") {
    testAssociation(AType.GOBP)
  }
  
  test("CC GO terms") {
    testAssociation(AType.GOCC)
  }
  
  test("MF GO terms") {
    testAssociation(AType.GOMF)
  }
  
  test ("KEGG pathways") {
    testAssociation(AType.KEGG)
  }
  
  test ("UniProt") {
    testAssociation(AType.Uniprot)
  }
  
  test ("OrthProts") {
    testAssociation(AType.OrthProts)
  }
  
  test ("CHEMBL") {
    testAssociation(AType.Chembl)
  }
  
  test ("DrugBank") {
    testAssociation(AType.Drugbank)
  }
  
  test ("Genes for pathway") {
    val ps = s.probesForPathway(f, "Glutathione metabolism")
    println(ps.size + " probes")
    assert(ps.size === 42)
  }
}