package otg.viewer.server.rpc

import otg.viewer.server.AppInfoLoader;
import t.intermine.MiRNATargets
import t.platform.mirna.MiRDBConverter
import t.platform.mirna.TargetTable
import t.platform.mirna.TargetTableBuilder
import t.viewer.shared.mirna.MirnaSource
import t.viewer.server.Configuration
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

class NetworkServiceImpl extends t.viewer.server.rpc.NetworkServiceImpl
  with OTGServiceServlet {

  var mirdbTableFut: Future[TargetTable] = _
  var mirtarbaseTableFut: Future[TargetTable] = _

  override def localInit(c: Configuration) {
    super.localInit(c)
    mirdbTableFut = Future {
      readTargetTable(
        s"$mirnaDir/mirdb_filter.txt",
        new MiRDBConverter(_, "MiRDB 5.0").makeTable)
    }
    mirtarbaseTableFut = Future {
       readTargetTable(
      s"$mirnaDir/tm_mirtarbase.txt",
      MiRNATargets.tableFromFile(_))
    }
  }

  protected def readTargetTable(file: String, doRead: String => TargetTable) = {
    println(s"Begin reading $file")
    blocking {
      val t = doRead(file)
      println(s"Read ${t.size} miRNA targets from $file")
      t
    }
  }

  def reverse[A,B](xs: Map[A,B]) = xs.map(x => (x._2 -> x._1))

  def resultAsOption[T](fut: Future[T]) = {
    val r = Await.ready(fut, Duration.Inf).value.get
    r match {
      case Success(t) => Some(t)
      case Failure(e) => None
    }
  }

  def mirtarbaseTable = resultAsOption(mirtarbaseTableFut)
  def mirdbTable = resultAsOption(mirdbTableFut)

  protected def mirnaTargetTable(source: MirnaSource) = {
    val table = source.id match {
      case MiRDBConverter.mirdbGraph       => mirdbTable
      case AppInfoLoader.TARGETMINE_SOURCE => mirtarbaseTable
      case _                               => throw new Exception("Unexpected MiRNA source")
    }
    table match {
      case Some(t) =>
        Some(t.scoreFilter(source.limit))
      case None =>
        Console.err.println(s"MirnaSource target table unavailable for ${source.id}")
        None
    }
  }
}
