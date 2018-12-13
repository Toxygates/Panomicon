/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otg.viewer.server

import scala.collection.JavaConverters._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import otg.OTGBConfig
import t.TTestSuite
import t.common.shared.AType
import t.model.SampleClass
import t.sparql.SampleFilter
import t.sparql.secondary._
import otg.model.sample.OTGAttribute
import t.model.sample.Attribute
import otg.sparql._
import t.viewer.server.Platforms
import t.platform.mirna.TargetTable

object AssociationResolverTest {

  val testClass: Map[Attribute, String] = Map(OTGAttribute.Repeat -> "Single",
    OTGAttribute.Organism -> "Rat",
    OTGAttribute.Organ -> "Liver",
    OTGAttribute.TestType -> "in vivo")

  def testSampleClass = new SampleClass(testClass.asJava)
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
  val probeStore = new OTGProbes(tsc)
  val sampleStore = new OTGSamples(baseConf)
  val b2rKegg = new B2RKegg(tsc.triplestore)
  val uniprot = new LocalUniprot(tsc.triplestore)
  val chembl = new ChEMBL()
  val drugBank = new DrugBank()

  def ar(types: Array[AType]) = new AssociationResolver(probeStore,
      sampleStore,
      new t.viewer.server.Platforms(Map()),
      b2rKegg, uniprot, chembl, drugBank,
      TargetTable.empty, None,
      sc, types, probes
      )(SampleFilter())

  private def testAssociation(typ: AType) = {
    val as = ar(Array(typ)).resolve
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
