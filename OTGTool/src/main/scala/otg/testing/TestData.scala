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

package otg.testing

import otg.OTGSeries
import otg.db.Metadata
import otg.db.OTGParameterSet
import t.Factory
import t.db.BasicExprValue
import t.db.ParameterSet
import t.db.Sample
import t.db.SampleParameter
import t.db.SeriesPoint
import t.db.testing.TestData.enumMaps

import scala.collection.JavaConversions._

object TestData {
  import t.db.testing.TestData._

  def mkPoint(pr: String, t: Int) = {
     val e = randomExpr()
     SeriesPoint(t, new BasicExprValue(e._1, e._2, pr))
  }

  def mkPoints(pr: String): Seq[SeriesPoint] = {
    val indepPoints = enumMaps("exposure_time").filter(_._1 != "9 hr").map(_._2)
    indepPoints.map(t => mkPoint(pr, t)).toSeq
  }

  lazy val series = for (compound <- enumValues("compound_name");
    doseLevel <- enumValues("dose_level");
    repeat <- enumValues("sin_rep_type");
    organ <- enumValues("organ_id");
    organism <- enumValues("organism");
    testType <- enumValues("test_type");
    probe <- probes;
    points = mkPoints(probeMap.unpack(probe))
    ) yield OTGSeries(repeat, organ, organism, probe,
        compound, doseLevel, testType, points)

  def metadata: Metadata = new Metadata {
    def samples = t.db.testing.TestData.samples

    def mapParameter(fact: Factory, key: String, f: String => String) = ???

    def parameterValues(identifier: String): Set[String] =
      enumMaps(identifier).keySet

    def parameters: ParameterSet = ???

    def parameters(s: Sample): Iterable[(SampleParameter, String)] = {
      samples.find(_ == s).get.sampleClass.constraints.toSeq.map(x =>  {
         val k = OTGParameterSet.byId(x._1)
         (k, x._2)
      })
    }
  }
}
