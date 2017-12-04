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

import scala.collection.JavaConversions._
import org.junit.runner.RunWith
import t.TTestSuite
import org.scalatest.junit.JUnitRunner
import t.model.SampleClass
import t.model.sample.BasicAttribute
import t.model.sample.Attribute

@RunWith(classOf[JUnitRunner])
class SampleClassTest extends TTestSuite {

  val x = new BasicAttribute("x", "x", false, null)
  val y = new BasicAttribute("y", "y", false, null)
  val z = new BasicAttribute("y", "y", false, null)
  val c = new BasicAttribute("a", "a", false, null)
  val b = new BasicAttribute("b", "b", false, null)

  val testMap: Map[Attribute, String] = Map(x -> "x",
      y -> "y",
      z -> "z")

  def scWith(m: Map[Attribute, String]) =
    new SampleClass(mapAsJavaMap(m))

  val testSc = scWith(testMap)
  val small = scWith(Map(x -> "x"))
  val big = scWith(testMap + (c -> "a"))
  val unrel = scWith(Map(b -> "b"))
  val incomp = scWith(Map(x -> "y"))

  test("equality") {
    assert(scWith(testMap) == testSc)
    assert(testSc != small)
    assert(testSc != big)
  }

  test("compatible") {
    assert(testSc.compatible(big))
    assert(big.compatible(testSc))
    assert(testSc.compatible(small))
    assert(small.compatible(testSc))
    assert(testSc.compatible(testSc))
    assert(testSc.compatible(unrel))
    assert(unrel.compatible(testSc))
    assert(!testSc.compatible(incomp))
    assert(!incomp.compatible(testSc))
  }

  test("collect") {
    assert(SampleClass.collect(seqAsJavaList(List(testSc, incomp)),
      new BasicAttribute("x", "whatever", false, null)).toSet
        == Set("x", "y"))
  }
}
