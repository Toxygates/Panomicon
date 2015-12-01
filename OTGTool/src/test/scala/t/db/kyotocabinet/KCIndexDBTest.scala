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

package t.db.kyotocabinet

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.TestData

@RunWith(classOf[JUnitRunner])
class KCIndexDBTest extends TTestSuite {
  import KCDBTest._
  import TestData._

  test("basic") {
    val db = memDBHash
    val kci = new KCIndexDB(memDBHash)
    for (k <- probeMap.tokens) {
      kci.put(k)
    }
    var m = kci.fullMap
    m.keys.toSet should equal(probeMap.tokens)
    m.values.toSet should equal ((0 until probeMap.data.size).toSet)

    val remove = TestData.probeMap.tokens.take(5)
    val removed = probeMap.tokens -- remove
    val removedKeys = probeMap.keys -- remove.map(m)

    for (k <- remove) {
      kci.remove(k)
    }
    m = kci.fullMap
    m.keys.toSet should equal(removed)
    m.values.toSet should equal(removedKeys)

    kci.release
  }

  test("named enum") {
    //TODO
  }

}
