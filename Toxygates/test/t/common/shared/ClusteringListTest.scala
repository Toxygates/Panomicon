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

package t.common.shared

import t.clustering.shared._
import org.junit.runner.RunWith
import t.TTestSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ClusteringListTest extends TTestSuite {

  val items = List(new StringList("type", "list1", Array("a", "b", "c")), new StringList("type", "list2", Array("d", "e", "f")))
  val algorithm = new Algorithm(Methods.WARD_D, Distances.COERRELATION, Methods.WARD_D2, Distances.EUCLIDIAN)

  import ClusteringList._

  test("basic") {
    val l = new ClusteringList(USER_CLUSTERING_TYPE,
        "test.name", algorithm, items.toArray)
    l.addParam("cutoff", "1.0")
    assert(l.size() === items.size)
    assert(l.items() === items.toArray)
    val p = l.pack()
    val up = ItemList.unpack(p)
    assert(up.name() === l.name())
    assert(up.`type` === l.`type`)
    assert(up.asInstanceOf[ClusteringList].algorithm() == algorithm)
    assert(up.asInstanceOf[ClusteringList].params().size == 1)
    assert(up.asInstanceOf[ClusteringList].params().get("cutoff") == "1.0")
  }

}
