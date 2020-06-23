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

import t.db.{ProbeIndex, Sample}
import t.model.sample.CoreParameter._
import t.model.sample.OTGAttribute._
import t.model.sample.{Attribute, OTGAttribute}
import t.platform._
import t.platform.mirna._
import t.testing.FakeContext

/**
 * Data for testing interaction networks.
 * We build on the existing TestData but extend it with another platform and more samples,
 * as well as relations between the two platforms.
 */
object NetworkTestData {
  val refseqIds = DBTestData.platform(1000, "refseq")
  val mirnaIds = DBTestData.platform(1000, "mirna")
  val mrnaIds = DBTestData.platform(1000, "mrna")

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
      platform = DBTestData.mrnaPlatformId)
  })
  val mirnaProbes = mirnaIds.map(m => {
    Probe(m, platform = mirnaPlatformId)
  })
  val probes = mrnaProbes.map(_.identifier) ++ mirnaProbes.map(_.identifier)
  implicit val probeIndex = new ProbeIndex(Map() ++ probes.zipWithIndex)

  val ids = (DBTestData.samples.size + 1 until DBTestData.samples.size + 1000).
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
          ControlGroup -> DBTestData.cgroup(time, "acetaminophen"));
    s = Sample(ids.next, values)
  ) yield s

  val samples = DBTestData.samples.toSeq ++ mirnaSamples
  val sampleIndex = DBTestData.sampleIndex(samples)

  implicit val context = new FakeContext

  def populate() {
    val mirnaData = DBTestData.makeTestData(false, mirnaSamples, mirnaIds)
    val mrnaData = DBTestData.makeTestData(false, DBTestData.samples, mrnaIds)
    context.populate(mirnaData)
    context.populate(mrnaData)
  }

}
