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

package t.common.shared

import org.junit.runner.RunWith

import t.TTestSuite
import t.common.server.GWTUtils._
import t.model.SampleClass
import t.model.sample.BasicAttribute
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SampleMultiFilterTest extends TTestSuite {

  def jset(x: String) = {
    val r = new java.util.HashSet[String]
    r.add(x)
    r
  }

  def jmap[T,U](x: Map[T, U]): java.util.Map[T, U] =
    x.asGWT

  val x = new BasicAttribute("x", "x", false, null)
  val y = new BasicAttribute("y", "y", false, null)
  val z = new BasicAttribute("y", "y", false, null)
  val c = new BasicAttribute("a", "a", false, null)
  val b = new BasicAttribute("b", "b", false, null)

  test("Empty") {
    val smf = new SampleMultiFilter(jmap(Map()))
    val sc = new SampleClass(jmap(Map(c -> "x")))
    assert(smf.accepts(sc))
  }

  test("Accept") {
    val smf = new SampleMultiFilter(jmap(Map(c -> jset("x"))))
    smf.addPermitted(c, "y")
    val sc = new SampleClass(jmap(Map(c -> "x")))
    assert(smf.accepts(sc))
  }

  test("Fail 1") {
    val smf = new SampleMultiFilter(jmap(Map()))
    smf.addPermitted(c, "y")
    val sc = new SampleClass(jmap(Map(c -> "x")))
    assert(!smf.accepts(sc))
  }

  test("Fail 2") {
    val smf = new SampleMultiFilter(jmap(Map()))
    smf.addPermitted(c, "y")
    val sc = new SampleClass(jmap(Map(b -> "y")))
    assert(!smf.accepts(sc))
  }
}
