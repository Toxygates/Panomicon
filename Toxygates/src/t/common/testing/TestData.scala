package t.common.testing

import t.db.RawExpressionData
import t.common.shared.sample.ExprMatrix
import t.common.shared.sample.Group
import otgviewer.server.rpc.Conversions._

/**
 * @author johan
 */
object TestData {
  import t.db.testing.{TestData => TTestData}
  val dataSchema = new TestSchema()

  def exprMatrix: ExprMatrix = exprMatrix(TTestData.makeTestData(true))

  def exprMatrix(rd: RawExpressionData): ExprMatrix = {
    val probes = rd.probes.toSeq
    val samples = rd.samples.toSeq
    val rows = for (p <- probes) yield samples.map(s => rd.asExtValues(s)(p))
    ExprMatrix.withRows(rows, probes, samples.map(_.sampleId))
  }

  val groups = TTestData.samples.take(10).grouped(2).zipWithIndex.map(ss => {
    val sss = ss._1.map(s => asJavaSample(s))
    new Group(dataSchema, "Gr" + ss._2, sss.toArray)
  }).toSeq

}
