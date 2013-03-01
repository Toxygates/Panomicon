package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import otgviewer.shared.DataFilter
import otgviewer.shared.CellType
import otgviewer.shared.Organ
import otgviewer.shared.Organism
import otgviewer.shared.RepeatType
import otgviewer.shared.AType
import org.scalatest.BeforeAndAfter

object SparqlServiceTest {
  def testFilter = new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat)
}

@RunWith(classOf[JUnitRunner])
class SparqlServiceTest extends FunSuite with BeforeAndAfter {

  var s: SparqlServiceImpl = _
  
  before {    
    s = new SparqlServiceImpl()
    s.localInit
  }  
  
  after {
    s.destroy
  }
  
  val f = SparqlServiceTest.testFilter
  
  val probes = Array("1387936_at", "1391544_at")
  val geneIds = Array("361510", "362972")
  
  private def testAssociation(typ: AType) = s.associations(f, Array(typ), probes, geneIds)
    
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
  
  test ("KO") {
    testAssociation(AType.KOProts)
  }
  
  test ("CHEMBL") {
    testAssociation(AType.Chembl)
  }
  
  test ("DrugBank") {
    testAssociation(AType.Drugbank)
  }
}