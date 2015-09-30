package t.db.testing

import t.db.MemoryLookupMap
import t.db.ProbeMap
import t.db.Sample
import t.db.BasicExprValue
import t.db.MatrixContext
import t.db.SampleMap

object TestData extends MatrixContext {
  val probeIds = (1 to 100)

  implicit val probeMap = {
    val pmap = Map() ++ probeIds.map(x => ("probe_" + x -> x))
    new MemoryLookupMap(pmap) with ProbeMap
  }

  val probes = probeIds.map(probeMap.unpack)

  val samples = (1 to 100).map(s => Sample(s"x$s"))

  val sampleMap = {
    val samples = Map() ++ (1 to 100).map(s => (s"x$s" -> s))
    new MemoryLookupMap(samples) with SampleMap
  }

  lazy val exprRecords = for (
    p <- probeMap.keys.toSeq; s <- samples;
    v = Math.random() * 100
  ) yield (s, p, BasicExprValue(v, 'P', probeMap.unpack(p)))

  lazy val matrix = new FakeBasicMatrixDB(exprRecords)

  def enumMaps = Map()

  def absoluteDBReader = matrix
  def foldsDBReader = { throw new Exception("Implement me") }
  def seriesBuilder = { throw new Exception("Implement me") }
}
