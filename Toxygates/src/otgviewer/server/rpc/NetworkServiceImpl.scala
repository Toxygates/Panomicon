package otgviewer.server.rpc

import t.viewer.shared.mirna.MirnaSource
import t.viewer.shared.TimeoutException
import t.platform.mirna.MiRDBConverter
import t.viewer.server.Configuration
import t.viewer.shared.NoDataLoadedException
import t.viewer.server.rpc.MatrixState
import t.sparql.Probes
import t.platform.mirna.TargetTableBuilder
import otgviewer.server.AppInfoLoader

class NetworkServiceImpl extends t.viewer.server.rpc.NetworkServiceImpl
  with OTGServiceServlet {

  lazy val mirdbTable = try {
    val file = s"$mirnaDir/mirdb_filter.txt"
    val t = new MiRDBConverter(file, "MiRDB 5.0").makeTable
    println(s"Read ${t.size} miRNA targets from $file")
    Some(t)
  } catch {
    case e: Exception =>
      e.printStackTrace()
      None
  }

  def loadMirnaTargetTable(source: MirnaSource, into: TargetTableBuilder) {
     source.id match {
       case MiRDBConverter.mirdbGraph =>
         mirdbTable match {
           case Some(t) =>
             into.addAll(t.scoreFilter(source.limit))
           case None =>
             Console.err.println("mirDB table unavailable")
         }
       case AppInfoLoader.TARGETMINE_SOURCE =>
         Console.err.println("I don't know how to load TargetMine data yet.")
       case _ =>
     }
  }
}
