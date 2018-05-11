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

package otgviewer.server

import t.BaseConfig
import otg.sparql.OTGProbes
import t.viewer.server.Configuration

class AppInfoLoader(probeStore: OTGProbes,
                    configuration: Configuration, baseConfig: BaseConfig,
                    appName: String)
                    extends t.viewer.server.AppInfoLoader(probeStore,
                      configuration, baseConfig, appName) {

   override def staticAnnotationInfo: Seq[(String, String)] = {
     /*
     * Note: the only data sources hardcoded here should be the ones
     * whose provisioning is independent of SPARQL data that we
     * control. For example, the ones obtained solely from remote
     * sources.
     */
    Seq(
      ("ChEMBL", "Dynamically obtained from https://www.ebi.ac.uk/rdf/services/chembl/sparql"),
      ("DrugBank", "Dynamically obtained from http://drugbank.bio2rdf.org/sparql")
      )
  }
}
