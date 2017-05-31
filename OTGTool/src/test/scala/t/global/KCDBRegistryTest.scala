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

package t.global

import t.TTestSuite

class KCDBRegistryTest extends TTestSuite {
  val testFile = "test.kct#bnum=1000"
  val testFileShort ="test.kct"

  test("Basic") {
    //creates the file
    val w = KCDBRegistry.getWriter(testFile)
    w should not equal(None)

    KCDBRegistry.isInWriting(testFile) should equal(true)

    KCDBRegistry.getReadCount(testFile) should equal(0)
    val r = KCDBRegistry.getReader(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(1)
    assert (r.get eq w.get)

    KCDBRegistry.getReader(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(2)
    KCDBRegistry.getReadCount(testFileShort) should equal(2)

    KCDBRegistry.releaseReader(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(1)

    KCDBRegistry.releaseReader(testFile)
    KCDBRegistry.getReadCount(testFile) should equal(0)
    KCDBRegistry.getReadCount(testFileShort) should equal(0)

    KCDBRegistry.closeWriters()

    KCDBRegistry.isInWriting(testFile) should equal(false)
    KCDBRegistry.getReadCount(testFile) should equal(0)
  }

}
