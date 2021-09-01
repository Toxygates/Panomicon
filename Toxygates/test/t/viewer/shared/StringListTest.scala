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

package t.shared.viewer

import org.junit.runner.RunWith
import t.TTestSuite
import org.scalatest.junit.JUnitRunner
import t.gwt.viewer.client.storage.ItemListPacker

@RunWith(classOf[JUnitRunner])
class StringListTest extends TTestSuite {

  val items = List("a", "b", "c")

  test("basic") {
    val l = new StringList("probes", "test.name", items.toArray)
    assert(l.size() === items.size)
    assert(l.items() === items.toArray)
    val p = ItemListPacker.doPack(l)
    val up = ItemListPacker.doUnpack(p)
    assert(up.packedItems().toArray === items.toArray)
    assert(up.name() === l.name())
    assert(up.`type`() === l.`type`())
  }
}
