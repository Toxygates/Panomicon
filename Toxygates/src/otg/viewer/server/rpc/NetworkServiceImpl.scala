package otg.viewer.server.rpc

import otg.viewer.server.AppInfoLoader;
import t.intermine.MiRNATargets
import t.platform.mirna.MiRDBConverter
import t.platform.mirna.TargetTable
import t.platform.mirna.TargetTableBuilder
import t.viewer.shared.mirna.MirnaSource
import t.intermine.MiRawImporter

class NetworkServiceImpl extends t.viewer.server.rpc.NetworkServiceImpl
  with OTGServiceServlet {

  protected def tryReadTargetTable(file: String, doRead: String => TargetTable) =
    try {
      println(s"Try to read $file")
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

  lazy val mirtarbaseTable =
    tryReadTargetTable(
      s"$mirnaDir/tm_mirtarbase.txt",
      MiRNATargets.tableFromFile(_))

  lazy val miRawTable =
    tryReadTargetTable(
      s"$mirnaDir/miraw_hsa_targets.txt",
      MiRawImporter.makeTable("MiRaw 6_1_10_AE10 NLL", _))

  protected def mirnaTargetTable(source: MirnaSource) = {
    val table = source.id match {
      case AppInfoLoader.MIRDB_SOURCE      => mirdbTable
      case AppInfoLoader.TARGETMINE_SOURCE => mirtarbaseTable
      case AppInfoLoader.MIRAW_SOURCE      => miRawTable
      case _                               => throw new Exception("Unexpected MiRNA source")
    }
    table match {
      case Some(t) =>
        Option(source.limit) match {
          case Some(l) => Some(t.scoreFilter(l))
          case _       => Some(t)
        }
      case None =>
        Console.err.println(s"MirnaSource target table unavailable for ${source.id}")
        None
    }
  }
}
