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

package otg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.DBTestData
import t.model.shared.SampleClassHelper._
import t.testing.FakeContext
import t._

@RunWith(classOf[JUnitRunner])
class SeriesTest extends TTestSuite {
  import t.testing.{TestData => OData}
  implicit val context = new FakeContext()
  val cmap = context.enumMaps("compound_name")

  //Note: much code shared with KCSeriesDBTest - could factor out
  trait SeriesTestType {
    def name: String
    def seriesType: OTGSeriesType
    def builderType: OTGSeriesBuilder

    def inputSeries: Iterable[OTGSeries]
    def attributeValue: String
    def attribute = seriesType.independentVariable
  }

  object TimeSeriesTest extends SeriesTestType {
    val name = "time series"
    val seriesType = TimeSeries
    val builderType = OTGTimeSeriesBuilder
    val attributeValue = OData.absentTime

    val inputSeries = OData.series
  }

  object DoseSeriesTest extends SeriesTestType {
    val name = "dose series"
    val seriesType = DoseSeries
    val builderType = OTGDoseSeriesBuilder
    val attributeValue = OData.absentDose

    val inputSeries = OData.doseSeries
  }

  val testTypes = List(TimeSeriesTest, DoseSeriesTest)

  for (tt <- testTypes) {
    test(s"pack and build ${tt.name}") {
      for (s <- tt.inputSeries) {
        val p = tt.builderType.pack(s)
        val b = tt.builderType.build(p, s.probe)
        b should equal(s.copy(points = Seq()))
      }
    }
  }

  for (tt <- testTypes) {
    test(s"makeNew ${tt.name}") {
      context.populate()
      val meta = OData.metadata
      val indepVarMap = tt.seriesType.independentVariableMap

      val ss = tt.builderType.makeNew(context.foldsDBReader, meta)
      val data = context.testData
      for (
        s <- ss;
        const = s.constraints.filter(_._2 != null).toSet;
        pr = context.probeMap.unpack(s.probe);
        relSamples = meta.samples.filter(x =>
          const.subsetOf(x.sampleClass.asScalaMap.toSet))
      ) {

        val points = for (
          s <- relSamples;
          ev <- data.asExtValues(s).get(pr)
        ) yield s

        val expectedPointCodes = points.flatMap(x =>
          meta.sampleAttribute(x, tt.seriesType.independentVariable))

        s.points.map(_.code) should contain theSameElementsAs (
          expectedPointCodes.map(indepVarMap(_)))
      }

      for (s <- DBTestData.samples) {
        val x = tt.builderType.buildEmpty(s, meta)
        ss.exists(_.classCode == x.classCode) should be(true)
      }

      val xs = DBTestData.samples.map(x => tt.builderType.buildEmpty(x, meta))
      for (s <- ss) {
        xs.exists(_.classCode == s.classCode) should be(true)
      }

      ss.map(s => (s.probe, s.classCode)).toSeq.distinct should
        equal(ss.map(s => (s.probe, s.classCode)))
    }
  }
}
