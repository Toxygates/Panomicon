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

package t.db.testing

import kyotocabinet.DB
import t.db._
import t.testing.FakeContext
import t.platform.OrthologMapping
import t.model.sample.Attribute
import t.model.sample.CoreParameter._
import otg.model.sample.OTGAttribute._

object TestData {
  def pickOne[T](xs: Seq[T]): T = {
    val n = Math.random * xs.size
    xs(n.toInt)
  }

  private def mm(ss: Seq[String]) =
    Map() ++ ss.zipWithIndex

  val enumMaps = Map(
    Compound.id -> mm(Seq("acetaminophen", "methapyrilene", "cocoa", "water")),
    DoseLevel.id -> mm(Seq("Control", "Low", "Middle", "High", "Really high")),
    Organism.id -> mm(Seq("Giraffe", "Squirrel", "Rat", "Mouse", "Human")),
    ExposureTime.id -> mm(Seq("3 hr", "6 hr", "9 hr", "24 hr")),
    Repeat.id -> mm(Seq("Single", "Repeat")),
    Organ.id -> mm(Seq("Liver", "Kidney")),
    TestType.id -> mm(Seq("Vitro", "Vivo")))

  private def em(k: Attribute) = enumMaps(k.id).keySet
  private def em(k: String) = enumMaps(k).keySet
  def enumValues(key: String) = em(key)

  def calls = List('A', 'P', 'M')

  private def cgroup(time: String, compound: String) = {
    val c = (enumMaps(ExposureTime.id)(time)) * 100 +
      enumMaps("compound_name")(compound)
    "" + c
  }

  def randomNumber(mean: Double, range: Double) =
    Math.random * range + (mean - range/2)

  def liverWeight(dose: String, individual: String) =
    if (dose == "Control" || individual == "2")
      3 //healthy
    else
      randomNumber(5, 0.2) //abnormal individual_id 1, 3

  def kidneyWeight(dose: String, individual: String) = {
    val sigma = 0.1
    val mean = 2.0
    dose match {
      case "Control" => // Rigged for the above mean and standard deviation
        if (individual == "1")
          mean - sigma * 2.0 / scala.math.sqrt(2)
        else
          mean + sigma / scala.math.sqrt(2)
      case "Really high" =>
        2.19 // Out of normal range for unit search, but not sample search
      case "High" => // normal range for unit search
        if (individual == "1")
          2.0
        else if (individual == "2")
          2.5 // out of normal range for sample search
        else // (individual == "3")
          1.5 // ditto
      case "Middle" => // All 3 individuals (and unit) below range
          1.5
      case _ => // "Low" => healthy
        2.0
    }
  }

  val ids = (0 until (5 * 4 * 3 * 4)).toStream.iterator
  val samples = for (
    dose <- em(DoseLevel); time <- em(ExposureTime);
    ind <- Set("1", "2", "3"); compound <- em("compound_name");
    values: Map[Attribute, String] = Map(DoseLevel -> dose, Individual -> ind,
          ExposureTime -> time, Compound -> compound,
          Repeat -> "Single", Organ -> "Liver",
          TestType -> "Vivo", Organism -> "Rat",
          LiverWeight -> liverWeight(dose, ind).toString,
          KidneyWeight -> kidneyWeight(dose, ind).toString,
          ControlGroup -> cgroup(time, compound));
    s = Sample("s" + ids.next, values)
  ) yield s

  def randomExpr(): (Double, Char, Double) = {
    val v = Math.random * 100000
    val call = pickOne(calls)
    (v, call, Math.abs(Math.random))
  }

  def randomPExpr(probe: String): PExprValue = {
    val v = randomExpr
    new PExprValue(v._1, v._3, v._2, probe)
  }

  val probes = (0 until 500)

  implicit val probeMap = {
    val pmap = Map() ++ probes.map(x => ("probe_" + x -> x))
    new ProbeIndex(pmap)
  }

  val unpackedProbes = probes.map(probeMap.unpack)

  val dbIdMap = {
    val dbIds = Map() ++ samples.zipWithIndex.map(s => (s._1.sampleId -> s._2))
    new SampleIndex(dbIds)
  }

  def makeTestData(sparse: Boolean): RawExpressionData = {
    makeTestData(sparse, samples)
  }

  def makeTestData(sparse: Boolean, useSamples: Iterable[Sample]): RawExpressionData = {
    var testData = Map[Sample, Map[String, (Double, Char, Double)]]()
    for (s <- useSamples) {
      var thisProbe = Map[String, (Double, Char, Double)]()
      for (p <- probeMap.tokens) {
        if (!sparse || Math.random > 0.5) {
          thisProbe += (p -> randomExpr())
        }
      }
      testData += (s -> thisProbe)
    }
    new RawExpressionData {
      val d = testData
      def samples = d.keys.toSeq
      def data(s: Sample) = d(s)
    }
  }

  /**
   * Obtain a cache hash database (in-memory)
   */
  def memDBHash: DB = {
    val r = new DB
    r.open("*", DB.OCREATE | DB.OWRITER)
    r
  }

  /**
   * Ditto, cache tree database
   */
  def memDBTree: DB = {
     val r = new DB
    r.open("%", DB.OCREATE | DB.OWRITER)
    r
  }

  def populate(db: MatrixDBWriter[PExprValue], d: RawExpressionData) {
    val evs = d.asExtValues
    for ((s, vs) <- evs; (p, v) <- vs) {
      db.write(s, probeMap.pack(p), v)
    }
  }

  implicit val context = new FakeContext(dbIdMap, probeMap)

  val orthologs: OrthologMapping = {
    val n = 100
    val pmap = probeMap
    val orths = (0 until n).map(p =>
      List(pmap.unpack(p), pmap.unpack(p + n), pmap.unpack(p + n * 2)))
    OrthologMapping("test", orths)
  }
}
