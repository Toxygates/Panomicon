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

package t.platform.mirna

import t.TTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.TestData
import t.platform._
import t.db.testing.NetworkTestData

@RunWith(classOf[JUnitRunner])
class TargetTableTest extends TTestSuite {
   test("basic") {
     val pf1 = TestData.platform(100, "p1-")
     val pf2 = TestData.platform(100, "p2-")
     
     val tt = NetworkTestData.targetTable(pf1, pf2, 100, 0.5)
     val assocs = tt.toVector
     
     assert(tt === assocs)
     assert(tt.scoreFilter(90).toSet === assocs.filter(_._3 >= 90).toSet)
     assert(tt.scoreFilter(0).toSet === tt.toSet)
   }
}