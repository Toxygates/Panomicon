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

package t.server.viewer

import t.BaseConfig
import t.shared.common.{FirstKeyedPair, GWTTypes, Platform}
import t.platform.mirna.MiRDBConverter
import t.sparql.{ProbeStore, _}
import t.server.viewer.Conversions._
import t.viewer.shared.{AppInfo, StringList}
import t.viewer.shared.clustering.ProbeClustering
import t.viewer.shared.mirna.MirnaSource
import t.server.common.GWTUtils._
import scala.collection.JavaConverters._

class AppInfoLoader(probeStore: ProbeStore,
                    configuration: Configuration,
                    baseConfig: BaseConfig) {

  import GWTTypes._

  /**
   * Called when AppInfo needs a full refresh.
   */
  def load(): AppInfo = {
    val probeLists = predefProbeLists()

    new t.sparql.PlatformStore(baseConfig).populateAttributes(baseConfig.attributes)

    new AppInfo(configuration.instanceName,
      sPlatforms(), probeLists,
      configuration.intermineInstances.toArray,
      probeClusterings(probeLists.asScala),
      configuration.applicationName,
      getAnnotationInfo,
      baseConfig.attributes,
      getMirnaSourceInfo)
  }

  def predefProbeLists() = {
    val ls = probeStore.probeLists(configuration.instanceURI).
      mapInnerValues(p => p.identifier)
    val sls = ls.map(x => new StringList(
      StringList.PROBES_LIST_TYPE, x._1, x._2.toArray)).toList
    sls.sortBy(_.name).asGWT
  }

  def probeClusterings(probeLists: Iterable[StringList]) = {
    val cls = probeLists.flatMap(x => Option(ProbeClustering.buildFrom(x)))

    cls.toSeq.asGWT
  }

  /**
   * Obtain data sources information for AppInfo
   */
  private def getAnnotationInfo: Array[Array[String]] = {
    val dynamic = probeStore.annotationsAndComments.toArray
    val static = staticAnnotationInfo
    Array(
      (dynamic ++ static).map(_._1),
      (dynamic ++ static).map(_._2))
  }

  def staticAnnotationInfo: Seq[(String, String)] = {
    /*
     * Note: the only data sources hardcoded here should be the ones
     * whose provisioning is independent of SPARQL data that we
     * control. For example, the ones obtained solely from remote
     * sources.
     */
    Seq(
      ("ChEMBL", "Dynamically obtained from https://www.ebi.ac.uk/rdf/services/chembl/sparql"),
      ("DrugBank", "Dynamically obtained from http://drugbank.bio2rdf.org/sparql"))
  }

  /**
   * "shared" platforms
   */
  def sPlatforms(): Array[Platform] = {
    val platforms = new t.sparql.PlatformStore(baseConfig) with SharedPlatforms
    platforms.sharedList.toArray
  }

  protected def getMirnaSourceInfo: Array[MirnaSource] = {
    MirnaSources.all.toArray
  }
}

object MirnaSources {
  //ID strings for the various miRNA sources that we support.

  val TARGETMINE_SOURCE: String = "TargetMine"
  val MIRDB_SOURCE: String = "miRDB"
  val MIRAW_SOURCE: String = "MiRAW"

  /**
   * MiRNA sources that are hardcoded into the application.
   */
  def all: Seq[MirnaSource] = {
    val mtbLevels = t.intermine.MiRNATargets.supportLevels.toSeq.sortBy(_._2).reverse.map(x =>
      new FirstKeyedPair(x._1, asJDouble(x._2))).asGWT

    /*
     * Sizes obtained through:
     * $wc -l tm_mirtarbase.txt
     * $wc -l mirdb_filter.txt
     */
    Seq(
      new MirnaSource(TARGETMINE_SOURCE, "miRTarBase (via TargetMine)", true, 3,
        1188967, "Experimentally verified", mtbLevels,
        "http://mirtarbase.mbc.nctu.edu.tw/php/index.php"),
      new MirnaSource(MIRDB_SOURCE, "miRDB 5.0", true, 90,
        3117189, "Predicted, score 0-100", null,
        "http://mirdb.org"),
      new MirnaSource(MIRAW_SOURCE, "MiRAW 6_1_10_AE10 NLL", false, null,
        1557789, "Predicted", null,
        "https://bitbucket.org/bipous/miraw_data"))
  }
}