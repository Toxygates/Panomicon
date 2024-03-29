/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.server.viewer

import t.shared.common.AType
import t.model.SampleClass
import t.model.sample.{Attribute, OTGAttribute}
import t.sparql.secondary._
import t.sparql.{ProbeStore, SampleFilter, SampleStore}
import t.{BaseConfig, TTestSuite}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._

object AssociationResolverTest {

  val testClass: Map[Attribute, String] = Map(OTGAttribute.Repeat -> "Single",
    OTGAttribute.Organism -> "Rat",
    OTGAttribute.Organ -> "Liver",
    OTGAttribute.TestType -> "in vivo")

  def testSampleClass = new SampleClass(testClass.asJava)
}

@RunWith(classOf[JUnitRunner])
class AssociationResolverTest extends TTestSuite {

  import t.server.viewer.testing.TestConfiguration

  def conf = TestConfiguration.config
  def baseConf = new BaseConfig(TestConfiguration.tc.tsConfig,
      TestConfiguration.tc.dataConfig)

  def sc = AssociationResolverTest.testSampleClass

  val probes = Array("1387936_at", "1391544_at")
//  val geneIds = Array("361510", "362972")

  val tsc = conf.tsConfig
  val probeStore = new ProbeStore(tsc)
  val sampleStore = new SampleStore(baseConf)
  val b2rKegg = new B2RKegg(tsc.triplestore)
  val uniprot = new LocalUniprot(tsc.triplestore)

  val ar = new AssociationResolver(probeStore, sampleStore, b2rKegg)

  private def testAssociation(typ: AType) = {
    val as = ar.resolve(Seq(typ), sc, SampleFilter(), probes)
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
}
