/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otgviewer.server.rpc

import scala.collection.JavaConversions._

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite

import t.viewer.server.Configuration
import t.common.shared.AType
import t.model.SampleClass

object SparqlServiceTest {
  val testClass = Map("sin_rep_type" -> "Single",
      "organism" -> "Rat",
      "organ_id" -> "Liver",
      "test_type" -> "in vivo")

  def testSampleClass = new SampleClass(testClass)
}

class SparqlServiceTest extends FunSuite with BeforeAndAfter {

  var s: SparqlServiceImpl = _
  before {
    val conf = t.viewer.testing.TestConfiguration.config
    s = new SparqlServiceImpl()
    s.localInit(conf)
  }

  after {
    s.destroy
  }

  val sc = SparqlServiceTest.testSampleClass

  val probes = Array("1387936_at", "1391544_at")
//  val geneIds = Array("361510", "362972")

  private def testAssociation(typ: AType) = s.associations(sc, Array(typ), probes)

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
    val ps = s.probesForPathway(sc, "Glutathione metabolism")
    println(ps.size + " probes")
    assert(ps.size === 42)
  }
}
