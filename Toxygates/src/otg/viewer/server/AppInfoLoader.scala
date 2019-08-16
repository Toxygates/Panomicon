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

package otg.viewer.server

import t.BaseConfig
import otg.sparql.OTGProbes
import otg.viewer.server.AppInfoLoader._
import t.viewer.server.Configuration
import t.viewer.shared.mirna.MirnaSource
import t.intermine.MiRNATargets
import t.common.server.GWTUtils._
import t.viewer.server.Conversions._
import t.common.shared.FirstKeyedPair
import t.platform.mirna.MiRDBConverter

object AppInfoLoader {
  //ID strings for the various miRNA sources that we support.

  val TARGETMINE_SOURCE: String = "TargetMine"
  val MIRDB_SOURCE: String = MiRDBConverter.mirdbGraph
  val MIRAW_SOURCE: String = "MiRAW"
}

class AppInfoLoader(probeStore: OTGProbes,
  configuration: Configuration, baseConfig: BaseConfig,
  appName: String)
    extends t.viewer.server.AppInfoLoader(probeStore,
      configuration, baseConfig, appName) {

  import AppInfoLoader._

  override def staticAnnotationInfo: Seq[(String, String)] = {
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

  override protected def staticMirnaSources: Seq[MirnaSource] = {
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
      new MirnaSource(MIRDB_SOURCE, "MirDB 5.0", true, 90,
        3117189, "Predicted, score 0-100", null,
        "http://mirdb.org"),
      new MirnaSource(MIRAW_SOURCE, "MiRAW 6_1_10_AE10 NLL", false, null,
        1557789, "Predicted", null,
        "https://bitbucket.org/bipous/miraw_data"))
  }
}
