package otgviewer.client

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import t.common.shared.StringList
import t.common.shared.ClusteringList

import scala.collection.JavaConversions._
import otgviewer.client.components.Screen
import t.viewer.client.HasLogger
import java.util.logging.Logger
import org.scalatest.Matchers

@RunWith(classOf[JUnitRunner])
class StringListsStoreHelperTest extends FunSuite with Matchers {

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
