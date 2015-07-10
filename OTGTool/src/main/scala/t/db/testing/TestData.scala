package t.db.testing

import t.db.MemoryLookupMap
import t.db.ProbeMap
import t.db.Sample
import t.db.BasicExprValue

object TestData {
  val probes = (1 to 100)
  implicit val probeMap = {
    val pmap = Map() ++ probes.map(x => ("probe_" + x -> x))
    new MemoryLookupMap(pmap) with ProbeMap
  }

  lazy val samples = (1 to 100).map(s => Sample(s"x$s"))

  lazy val exprRecords = for (
    p <- probeMap.keys.toSeq; s <- samples;
    v = Math.random() * 100
  ) yield (s, p, BasicExprValue(v, 'P'))

  lazy val matrix = new FakeBasicMatrixDB(exprRecords)
}
