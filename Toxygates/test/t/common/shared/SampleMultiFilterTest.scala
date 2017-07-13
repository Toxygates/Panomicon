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

package t.common.shared

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import t.model.SampleClass

class SampleMultiFilterTest extends FunSuite {

  def jset(x: String) = {
    val r = new java.util.HashSet[String]
    r.add(x)
    r
  }

  def jmap[T,U](x: Map[T, U]): java.util.Map[T, U] =
    new java.util.HashMap(mapAsJavaMap(x))

  test("Empty") {
    val smf = new SampleMultiFilter(jmap(Map()))
    val sc = new SampleClass(jmap(Map("a" -> "x")))
    assert(smf.accepts(sc))
  }

  test("Accept") {
    val smf = new SampleMultiFilter(jmap(Map("a" -> jset("x"))))
    smf.addPermitted("a", "y")
    val sc = new SampleClass(jmap(Map("a" -> "x")))
    assert(smf.accepts(sc))
  }

  test("Fail 1") {
    val smf = new SampleMultiFilter(jmap(Map()))
    smf.addPermitted("a", "y")
    val sc = new SampleClass(jmap(Map("a" -> "x")))
    assert(!smf.accepts(sc))
  }

  test("Fail 2") {
    val smf = new SampleMultiFilter(jmap(Map()))
    smf.addPermitted("a", "y")
    val sc = new SampleClass(jmap(Map("b" -> "y")))
    assert(!smf.accepts(sc))
  }
}
