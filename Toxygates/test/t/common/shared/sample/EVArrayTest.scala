/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.common.shared.sample

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.EVABuilder
import t.common.shared.sample.EVArray
import t.TTestSuite

@RunWith(classOf[JUnitRunner])
class EVArrayTest extends TTestSuite {

  test("basic") {
    val d = (1 to 3).map(x => new ExpressionValue(x, 'M', "tooltip"))
    val a = EVArray(d)
    assert(a === d)
    assert(a.length === 3)
    assert(a(0) === d(0))

    val x = new ExpressionValue(4, 'M', "tooltip")
    val big = a :+ x
    assert(a === d)
    assert(big === d :+ x)
    assert(big.length == 4)

    val b2 = EVArray(big)
    assert(b2 === big)
  }

  implicit def builder() = EVABuilder()

  test("append") {
    val d = (1 to 4).map(x => new ExpressionValue(x, 'M', "tooltip"))

    val b1 = EVArray(d take 2)
    val b2 = EVArray(d drop 2)
    val app = b1 ++ b2
    println(app)
    assert(app === d)
  }

  test("builder") {
    val d = (1 to 3).map(x => new ExpressionValue(x, 'M', "tooltip"))
    val ea = (EVABuilder() ++= d).result
    assert(ea === d)

    var b = EVABuilder()
    for (i <- d) {
      b += i
    }

    var b2 = EVABuilder()
    b2 ++= d
    assert(b.result === b2.result)
  }
}
