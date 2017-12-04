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

package otgviewer.server

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import t.TTestSuite
import t.common.shared.sample.Group
import t.common.testing.{TestData => OTestData}
import t.db.ExprValue
import t.db.testing.TestData
import t.viewer.server.Conversions._
import t.viewer.server._

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
    for (
      m <- os.mappings;
      ms = m.toSet;
      s <- d.samples
    ) {
      val data = d.dataMap.mapValues(vs => vs.filter(p => ms.contains(p._1)))
      val vs = data(s).toSeq.map(r => ExprValue(r._2._1, r._2._2, r._1))
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

    val ug = conv.rawUngrouped
    ug.rowKeys should equal(cur.rowKeys)
    ug.rowMap should equal(cur.rowMap)

    for (r <- cur.rowKeys) {
      val row = cur.row(r)
      val inDomain = pm.reverse(r).toSeq
      val domainRows = m.current.selectNamedRows(inDomain).toRowVectors
      for (i <- 0 until cur.columns; if (row(i).present)) {
        val present = domainRows.map(_(i)).filter(_.present)
        medianCompatible(row(i), present)
      }
    }

  }
}
