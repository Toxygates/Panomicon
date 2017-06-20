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

package otgviewer.client

import java.util.logging.Logger

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.seqAsJavaList

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import t.common.client.HasLogger
import t.common.shared.ClusteringList
import t.common.shared.StringList
import t.TTestSuite

@RunWith(classOf[JUnitRunner])
class StringListsStoreHelperTest extends TTestSuite {

  import java.util.{ ArrayList => JList }

  val clusters = Seq(
    Seq("p1", "p2", "p3"),
    Seq("p4", "p5"),
    Seq("p6", "p7"))

  val stringLists = clusters.zipWithIndex map {
    case (cl, i) =>
      new StringList(StringList.PROBES_LIST_TYPE,
        s"Cluster $i", cl.toArray)
  }

  val logger = new HasLogger {
    def getLogger = Logger.getGlobal
  }

  test("basic") {
    val clustering = new ClusteringList(
      ClusteringList.USER_CLUSTERING_TYPE,
      "testClusters", null, stringLists.toArray)

    val compiled = StringListsStoreHelper.compileLists(
        Seq(clustering))
    println(compiled)

    val rebuild = StringListsStoreHelper.rebuildLists(logger, compiled)
    println(rebuild)

    var lists1 = rebuild(0).asInstanceOf[ClusteringList].asStringLists
    var lists2 = clustering.asStringLists

    println(lists1.toVector)
    println(lists2.toVector)

    assert(lists1 === lists2)

    //shuffle the order
    val shuffled = compiled.drop(1) :+ compiled.head
    val rebuild2 = StringListsStoreHelper.rebuildLists(logger, shuffled)

    lists1 = rebuild2(0).asInstanceOf[ClusteringList].asStringLists
    assert(lists1 === lists2)
  }
}
