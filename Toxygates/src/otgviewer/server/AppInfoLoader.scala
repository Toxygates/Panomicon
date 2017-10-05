package otgviewer.server

import t.BaseConfig
import otg.sparql.Probes
import t.viewer.server.Configuration
import t.viewer.shared.mirna.MirnaSource

class AppInfoLoader(probeStore: Probes,
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
      ("DrugBank", "Dynamically obtained from http://drugbank.bio2rdf.org/sparql"))
  }

  private val TARGETMINE_SOURCE = "TargetMine"
  override protected def staticMirnaSources: Seq[MirnaSource] = {
    val size = 0 //TODO
    Seq(
      new MirnaSource(TARGETMINE_SOURCE, "miRTarBase (via TargetMine)", true, true, 0.5, size))
  }
}
