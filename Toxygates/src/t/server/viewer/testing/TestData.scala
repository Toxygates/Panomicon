package t.server.viewer.testing

import t.server.viewer.Conversions.asJavaSample
import t.shared.common.sample.Group

/**
 * @author johan
 */
object TestData {

  import t.db.testing.{DBTestData => TTestData}

  val dataSchema = new TestSchema()

  val groups = TTestData.samples.take(10).grouped(2).zipWithIndex.map(ss => {
    val sss = ss._1.map(s => asJavaSample(s))
    new Group(dataSchema, "Gr" + ss._2, sss.toArray)
  }).toSeq

}
