package otgviewer.server

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.TestData
import t.common.testing.{TestData => OTestData}
import t.common.shared.probe.OrthologProbeMapper
import t.common.shared.probe.MedianValueMapper
import t.db.ExprValue
import t.common.shared.sample.Group
import otgviewer.server.rpc.Conversions._

/**
 * @author johan
 */
@RunWith(classOf[JUnitRunner])
class MatrixMapperTest extends TTestSuite {
  import TestData._

  val os = orthologs
  val pm = new OrthologProbeMapper(orthologs)
  val vm = MedianValueMapper

  test("probes") {
     for (m <- os.mappings; p <- m) {
       val f = pm.forward(p)
       pm.reverse(f) should equal(m)
     }
  }

  private def medianCompatible(x: ExprValue, test: Iterable[ExprValue]) {
    assert(test.exists(_.value == x.value) ||
        (test.size == 2 && test.map(_.value).sum / 2 == x.value))
  }

  test("values") {
    val d = makeTestData(false)
    for (m <- os.mappings; s <- d.samples;
      ms = m.toSet) {
      val data = d.data.mapValues(vs => vs.filter(p => ms.contains(p._1)))
      val vs = data(s).map(r => ExprValue(r._2._1, r._2._2, r._1))
      for (p <- m) {
        val filt = vs.filter(_.present)
        if (filt.size > 0) {
          val c = vm.convert(p, filt)
          //for size 2 we use mean rather than median.
          //call may change
          medianCompatible(c, filt)
        }
      }
    }
  }

  //TODO quite a lot of code here is shared with ManagedMatrixTest
  test("managedMatrix") {
    val mm = new MatrixMapper(pm, vm)
    val schema = OTestData.dataSchema
    val data = context.testData

    def foldBuilder = new ExtFoldBuilder(false, context.foldsDBReader,
      probes.map(probeMap.unpack))

    val groups = TestData.samples.take(10).grouped(2).zipWithIndex.map(ss => {
      val sss = ss._1.map(s => asJavaSample(s))
      new Group(schema, "Gr" + ss._2, sss.toArray)
    }).toSeq

    context.populate
    val m = foldBuilder.build(groups, false, true)

    val conv = mm.convert(m)
    val cur = conv.current

    assert(cur.rowKeys.toSet subsetOf pm.range.toSet)
    assert(cur.rowKeys.size == orthologs.mappings.size)
    cur.columnKeys.toSet should equal(groups.map(_.getName).toSet)

    val ug = conv.rawUngroupedMat
    ug.rowKeys should equal(cur.rowKeys)
    ug.rowMap should equal(cur.rowMap)

    for (r <- cur.rowKeys) {
      val row = cur.row(r)
      val inDomain = pm.reverse(r).toSeq
      val domainRows = m.current.selectNamedRows(inDomain).toRowVectors
      //size 2 uses mean instead of median
      //call may change.
      for (i <- 0 until cur.columns; if (row(i).present)) {
        val present = domainRows.map(_(i)).filter(_.present)
        medianCompatible(row(i), present)
      }
    }

  }
}
