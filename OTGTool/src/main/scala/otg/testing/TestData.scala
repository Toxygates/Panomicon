package otg.testing

import t.db.SeriesPoint
import otg.OTGSeries
import t.db.BasicExprValue

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
}
