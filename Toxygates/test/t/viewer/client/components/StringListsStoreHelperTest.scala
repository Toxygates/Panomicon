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

package t.viewer.client.components

import java.util.logging.Logger
import scala.collection.JavaConverters._
import org.junit.runner.RunWith
import t.TTestSuite
import t.clustering.shared.ClusteringList
import t.viewer.shared.ItemList
import t.viewer.shared.StringList
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringListsStoreHelperTest extends TTestSuite {

  val clusters = Seq(
    Seq("p1", "p2", "p3"),
    Seq("p4", "p5"),
    Seq("p6", "p7"))

  val stringLists = clusters.zipWithIndex map {
    case (cl, i) =>
      new StringList(StringList.PROBES_LIST_TYPE,
        s"Cluster $i", cl.toArray)
  }

  val logger = Logger.getGlobal

  test("basic") {
    val clustering = new ClusteringList(
      ClusteringList.USER_CLUSTERING_TYPE,
      "testClusters", null, stringLists.toArray)

    val compiled = StringListsStoreHelper.compileLists(
        Seq[ItemList](clustering).asJava)
    println(compiled)

    val rebuild = StringListsStoreHelper.rebuildLists(logger, compiled).asScala
    println(rebuild)

    var lists1 = rebuild(0).asInstanceOf[ClusteringList].asStringLists
    var lists2 = clustering.asStringLists

    println(lists1.toVector)
    println(lists2.toVector)

    assert(lists1 === lists2)

    val scCompiled = compiled.asScala
    //shuffle the order
    val shuffled = scCompiled.drop(1) :+ scCompiled.head
    val rebuild2 = StringListsStoreHelper.rebuildLists(logger, shuffled.asJava).asScala

    lists1 = rebuild2(0).asInstanceOf[ClusteringList].asStringLists
    assert(lists1 === lists2)
  }
}
