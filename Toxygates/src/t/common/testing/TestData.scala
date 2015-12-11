package t.common.testing

import t.db.RawExpressionData
import t.common.shared.sample.ExprMatrix

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

}
