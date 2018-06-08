/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otg

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import t.platform.Species._
import t.testing.TestConfig
import t.TTestSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class OTGContextTest extends TTestSuite {

  val context = new otg.testing.FakeContext

  test("probe map") {
    val m1 = context.probeMap

    println(m1.tokens.size)

    assert(m1.tokens.size === m1.keys.size)

    val probes1 = List(100, 1, 300, 122)

    println(m1.keys)

    for (p <- probes1) {
      val str = m1.unpack(p)
      assert(m1.pack(str) === p)
    }
  }
}
