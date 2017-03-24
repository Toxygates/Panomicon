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

package otg.sparql

import t.TTestSuite
import scala.collection.JavaConversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import otg.Species._
import t.platform.Probe
import t.sparql._
import t.sparql.Triplestore
import t.sparql.secondary._
import t.testing.TestConfig

@RunWith(classOf[JUnitRunner])
class SPARQLTest extends TTestSuite {

  val config = TestConfig.config

  val affyProbes = new Probes(config.triplestore)
  val homologene = new B2RHomologene
  val iproclass = new B2RIProClass
  val kegg = new B2RKegg(config.triplestore.triplestore)
  val chembl = new ChEMBL
  val drugbank = new DrugBank
  val uniprot = new OfficialUniprot

  after {
    affyProbes.close
    homologene.close
    iproclass.close
    chembl.close
    drugbank.close
    uniprot.close
  }

  test("Probe attributes") {
    val attrs = affyProbes.withAttributes(List(Probe("1397383_at"), Probe("1389556_at")))
    val r = attrs.map(p => p.genes.isEmpty || p.proteins.isEmpty || p.name == null).reduceLeft(_ || _)
    r should equal(false)
  }

  test("Homologene") {
    val hgs = homologene.homologousGenes(Gene("14854"))
    println(hgs)
    hgs.size should equal(21)
    val hgus = homologene.homologousGenesFor(List(Protein("P23219")))
    println(hgus)
    hgus.allValues.size should equal(9)
  }

  test("IPRoClass") {
    iproclass.geneIdsFor(List(Protein("Q197F8"))).size should equal(1)
  }

  import t.sparql.secondary.Compound
  test("ChEMBL") {
    val t1 = chembl.targetsFor(Compound.make("acetaminophen"))
    println(t1)
    t1.size should equal(10)
    //TODO this may return more hits now (3 species)
    val t2 = chembl.targetingFor(List("Q99685", "P07541").map(Protein(_)),
      List("tylEnol", "phenaphen", "Paracetamol").map(Compound.make))
    println(t2)
    t2.allValues.size should equal(3)
  }

  test("DrugBank") {
    val t1 = drugbank.targetsFor(Compound.make("niTrofurazone"))
    println(t1)
    t1.size should equal(4)
    val t2 = drugbank.targetsFor(Compound.make("acetaminophen"))
    println(t2)
    t2.size should equal(2)
    val t3 = drugbank.targetingFor(List("P61889", "P23219").map(Protein(_)),
      List("NitrOfurazone", "acetaminophen", "lornoxicam").map(Compound.make))

    println(t3)
    t3.size should equal(2)
  }

  test("Uniprot") {
    val kos = uniprot.keggOrthologs(Protein("Q21549"))
    println(kos)
    kos.size should equal(6)
    val ops = uniprot.orthologsFor(List(Protein("Q21549"), Protein("Q8DXM9")), Rat)
    println(ops)
    ops.allValues.size should equal(2)
  }

  test("B2RKegg") {
    //    val pws = kegg.forGenes(List(Gene("24379", Rat)), Rat)
    //    println(pws)
    //    pws.allValues.size should equal(5)

    //    val gs = kegg.withAttributes(pws.allValues, Rat).flatMap(_.genes)
    //    println(gs.size + " genes")
    //    gs.size should equal(115)
  }

}
