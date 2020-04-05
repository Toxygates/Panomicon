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

package otg.sparql

import org.scalatest.junit.JUnitRunner

import otg._
import t.platform.Species._
import t.TTestSuite
import t.sparql._
import t.sparql.secondary._
import t.testing.TestConfig
import t.platform.Probe
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AffyProbesTest extends TTestSuite {

  val config = TestConfig.config
  val affyProbes = new OTGProbeStore(config.triplestore)

  after {
    affyProbes.close
  }

  test("probe geneSyms") {
    val pr1 = Probe("1367453_at")
    val pr2 = Probe("1367456_at")
    val syms = toBioMap(affyProbes.withAttributes(Seq(pr1, pr2)), (p: Probe) => p.symbols)
    println(syms)
    assert(syms.size == 2)
    println(syms(pr1))
    println(syms(pr2))
    val g1 = Gene("a", symbol = "Cdc37")
    val g2 = Gene("b", symbol = "Ube2d3")
    assert(Set(g1).map(_.symbol) subsetOf syms(pr1))
    assert(Set(g2).map(_.symbol) subsetOf syms(pr2))
  }

  test("probe geneIds") {
    val p1 = Probe("1367453_at")
    val p2 = Probe("1367456_at")
    val genes = toBioMap(affyProbes.withAttributes(Array(p1, p2)), (p: Probe) => p.genes)
    println(genes)
    assert(Set(Gene("114562")) subsetOf genes(p1).toSet)
    assert(Set(Gene("641452"), Gene("81920")) subsetOf genes(p2).toSet)
  }

  test("probe uniprots") {
    val p1 = Protein("D3ZCV5")
    val p2 = Protein("D4A3D4")
    val probes = affyProbes.forUniprots(Set(p1, p2))
    println(probes)
    val pr1 = Probe("1368718_at")
    val pr2 = Probe("1382137_at")
    assert(Set(pr1, pr2) subsetOf probes.toSet)

    val reverse = affyProbes.withAttributes(Set(pr1, pr2))
    println(reverse)
    assert(Set(p1, p2) subsetOf reverse.flatMap(_.proteins).toSet)
  }

  test("probe title") {
    val pr = Probe("1007_s_at")
    val title = affyProbes.withAttributes(Set(pr))
    title.head.name should (equal("discoidin domain receptor tyrosine kinase 1")
      or equal("microRNA 4640"))
  }

  test("mixed identifiers") {
    implicit val context = new OTGMatrixContext(config)
    val idents = Array("g6pd", "1388833_at", "mgst2")
    val res = affyProbes.identifiersToProbes(context.probeMap, idents, true, false)
    assert(res.size >= 5)

    val idents2 = Array("gss", "gclc")
    val res2 = affyProbes.identifiersToProbes(context.probeMap, idents, true, false)
    assert(res2.size >= 5)
  }

  test("GO terms") {
    val gots = affyProbes.goTerms("catabolic")
    gots.size should equal(1000)
    println(gots)
  }

  test("Probes for GO term") {
    val pbs = affyProbes.forGoTerm(GOTerm("", "aromatic compound catabolic process"))
    assert(pbs.size >= 28)
    println(pbs)
  }
}
