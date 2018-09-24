package otgviewer.server.rpc

import otgviewer.server.AppInfoLoader
import t.intermine.MiRNATargets
import t.platform.mirna.MiRDBConverter
import t.platform.mirna.TargetTable
import t.platform.mirna.TargetTableBuilder
import t.viewer.shared.mirna.MirnaSource

class NetworkServiceImpl extends t.viewer.server.rpc.NetworkServiceImpl
  with OTGServiceServlet {

  protected def tryReadTargetTable(file: String, doRead: String => TargetTable) =
    try {
      val t = doRead(file)
      println(s"Read ${t.size} miRNA targets from $file")
      Some(t)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }

  lazy val mirdbTable =
    tryReadTargetTable(
      s"$mirnaDir/mirdb_filter.txt",
      new MiRDBConverter(_, "MiRDB 5.0").makeTable)

  lazy val targetmineTable =
    tryReadTargetTable(
      s"$mirnaDir/tm_mirtarbase.txt",
      MiRNATargets.tableFromFile(_))

  protected def mirnaTargetTable(source: MirnaSource) = {
    val table = source.id match {
      case MiRDBConverter.mirdbGraph       => mirdbTable
      case AppInfoLoader.TARGETMINE_SOURCE => targetmineTable
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
