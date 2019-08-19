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

package t.db.testing

import t.platform.mirna._
import t.platform._
import t.sparql.Platforms
import t.sparql.secondary.Compound
import t.db.Sample
import t.model.sample.Attribute
import t.model.sample.CoreParameter._
import otg.model.sample.OTGAttribute
import otg.model.sample.OTGAttribute._
import t.testing.FakeContext
import t.db.ProbeIndex

/**
 * Data for testing interaction networks.
 * We build on the existing TestData but extend it with another platform and more samples,
 * as well as relations between the two platforms.
 */
object NetworkTestData {
  val refseqIds = TestData.platform(1000, "refseq")
  val mirnaIds = TestData.platform(1000, "mirna")
  val mrnaIds = TestData.platform(1000, "mrna")

  def targetTable(mirnaPlatform: Iterable[String],
    refseqPlatform: Iterable[String],
    maxScore: Double, fraction: Double) = {
     val builder = new TargetTableBuilder

     val associations = for (p1 <- mirnaPlatform; p2 <- refseqPlatform;
       mirna = new MiRNA(p1); refseq = new RefSeq(p2);
       score = Math.random() * maxScore;
       frac = Math.random();
       if frac < fraction;
       info = new ScoreSourceInfo("pseudo")) {
       builder.add(mirna, refseq, score, info)
     }
     builder.build
  }

  val mirnaPlatformId = "mirnaTest"

  val targets = targetTable(mirnaIds, refseqIds, 100, 0.1)
  val mrnaProbes = mrnaIds.map(m => {
    Probe(m,
      transcripts = Seq(RefSeq(m.replace("mrna", "refseq"))),
      platform = TestData.mrnaPlatformId)
  })
  val mirnaProbes = mirnaIds.map(m => {
    Probe(m, platform = mirnaPlatformId)
  })
  val probes = mrnaProbes.map(_.identifier) ++ mirnaProbes.map(_.identifier)
  implicit val probeIndex = new ProbeIndex(Map() ++ probes.zipWithIndex)

  val ids = (TestData.samples.size + 1 until TestData.samples.size + 1000).
    map(i => s"mir-s$i").iterator

  val mirnaSamples = for (
    dose <- Seq("Control", "Middle"); time = "24 hr";
    ind <- Set("1", "2", "3");
    values: Map[Attribute, String] = Map(DoseLevel -> dose, Individual -> ind,
          ExposureTime -> time, OTGAttribute.Compound -> "acetaminophen",
          Repeat -> "Single", Organ -> "Liver",
          Type -> "miRNA",
          Platform -> "mirnaTest",
          TestType -> "Vivo", Organism -> "Rat",
          ControlGroup -> TestData.cgroup(time, "acetaminophen"));
    s = Sample(ids.next, values)
  ) yield s

  val samples = TestData.samples.toSeq ++ mirnaSamples
  val sampleIndex = TestData.sampleIndex(samples)

  implicit val context = new FakeContext(sampleIndex, probeIndex)

  def populate() {
    val mirnaData = TestData.makeTestData(false, mirnaSamples)
    val mrnaData = TestData.makeTestData(false, TestData.samples)
    context.populate(mirnaData)
    context.populate(mrnaData)
  }

}
