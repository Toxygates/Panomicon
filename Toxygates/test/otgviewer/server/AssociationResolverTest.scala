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

package otgviewer.server

import scala.collection.JavaConversions._
import t.common.shared.SampleClass
import t.common.shared.AType
import org.junit.runner.RunWith
import t.TTestSuite
import org.scalatest.junit.JUnitRunner
import otgviewer.server.rpc.SparqlServiceImpl
import t.viewer.shared.TimeoutException
import t.sparql.secondary.ChEMBL
import t.sparql.secondary.DrugBank
import t.sparql.secondary.Uniprot
import t.sparql.secondary.B2RKegg
import t.sparql.secondary.LocalUniprot
import t.sparql.SampleFilter
import otg.OTGBConfig

<<<<<<< local
object AssociationResolverTest {
=======
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite

import t.viewer.server.Configuration
import t.common.shared.AType
import t.model.SampleClass

object SparqlServiceTest {
>>>>>>> other
  val testClass = Map("sin_rep_type" -> "Single",
    "organism" -> "Rat",
    "organ_id" -> "Liver",
    "test_type" -> "in vivo")

  def testSampleClass = new SampleClass(testClass)
}

@RunWith(classOf[JUnitRunner])
class AssociationResolverTest extends TTestSuite {

  import t.viewer.testing.TestConfiguration
  def conf = TestConfiguration.config
  def baseConf = new OTGBConfig(TestConfiguration.tc.tsConfig,
      TestConfiguration.tc.dataConfig)

  def sc = AssociationResolverTest.testSampleClass

  val probes = Array("1387936_at", "1391544_at")
//  val geneIds = Array("361510", "362972")

  val tsc = conf.tsConfig
  val probeStore = new otg.sparql.Probes(tsc)
  val sampleStore = new otg.sparql.OTGSamples(baseConf)
  val b2rKegg = new B2RKegg(tsc.triplestore)
  val uniprot = new LocalUniprot(tsc.triplestore)
  val chembl = new ChEMBL()
  val drugBank = new DrugBank()

  def ar(types: Array[AType]) = new AssociationResolver(probeStore,
      sampleStore,
      b2rKegg, uniprot, chembl, drugBank,
      sc, types, probes
      )

  private def testAssociation(typ: AType) = {
    val as = ar(Array(typ)).resolve(SampleFilter())
    assert (as.size == 1)
    assert(as(0).`type`() == typ)
    assert(as(0).data.size > 0)
  }

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
//
//  test ("OrthProts") {
//    testAssociation(AType.OrthProts)
//  }

  test ("CHEMBL") {
    testAssociation(AType.Chembl)
  }

  test ("DrugBank") {
    testAssociation(AType.Drugbank)
  }
}
