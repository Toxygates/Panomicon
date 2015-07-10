package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.common.shared.sample.ExpressionValue
import t.db.testing._
import t.db.testing.TestData._
import org.scalatest.FunSuite
import otgviewer.shared.Group
import otgviewer.server.rpc.Conversions._

@RunWith(classOf[JUnitRunner])
class ManagedMatrixTest extends FunSuite {

  implicit val testContext = TestData
  val schema = t.common.testing.TestData.dataSchema()

  def normBuilder = new NormalizedBuilder(false, testContext.matrix,
      TestData.probes)

  val groups = TestData.samples.take(10).grouped(2).zipWithIndex.map(ss => {
    val sss = ss._1.map(s => asJavaSample(s))
    new Group(schema, "Gr" + ss._2, sss.toArray)
  }).toSeq

  test("build") {
    val m = normBuilder.build(groups, false, true)
    val cur = m.current
    assert (cur.rows === TestData.probes.size)
    assert (cur.columns === groups.size)

    val raw = m.rawUngroupedMat
    assert (raw.columns === 10)
    assert (raw.rows === cur.rows)

    val info = m.info
    val colNames = (0 until info.numColumns()).map(info.columnName)
    assert (colNames === (0 until 5).map(g => s"Gr$g"))

  }

//
//  test("adjoined sorting") {
//    val em = testMatrix
//    val adjData = List(List(3),
//         List(1),
//         List(2),
//         List(5),
//         List(4)).map(xs => EVArray(xs.map(new ExpressionValue(_))))
//    val em2 = ExprMatrix.withRows(adjData)
//
//  }
}
