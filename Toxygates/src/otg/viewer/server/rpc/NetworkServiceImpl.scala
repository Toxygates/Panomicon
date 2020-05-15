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

package otg.viewer.server.rpc

import otg.viewer.server.AppInfoLoader
import t.intermine.{MiRNATargets, MiRawImporter}
import t.platform.mirna.{MiRDBConverter, TargetTable}
import t.viewer.server.rpc.OTGServiceServlet
import t.viewer.shared.mirna.MirnaSource

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

  lazy val miRawTable = {
    val allTranscripts = platforms.data.valuesIterator.flatten.flatMap(_.transcripts).toSet

    tryReadTargetTable(
      s"$mirnaDir/miraw_hsa_targets.txt",
      MiRawImporter.makeTable("MiRaw 6_1_10_AE10 NLL", _, allTranscripts))
  }

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
