package otgviewer.server.rpc

import otg.sparql._
import otgviewer.server.AppInfoLoader
import t.common.shared.AType
import t.common.shared.sample._
import t.model.SampleClass
import t.platform.Probe
import t.sparql._
import t.sparql.secondary._
import t.viewer.server.Configuration
import t.viewer.server.Conversions._
import t.viewer.server.intermine.IntermineConnector
import t.viewer.server.intermine.Intermines
import t.viewer.shared.Association
import t.viewer.shared.TimeoutException
import t.viewer.server.rpc.NetworkState

class ProbeServiceImpl extends t.viewer.server.rpc.ProbeServiceImpl
  with OTGServiceServlet with otgviewer.client.rpc.ProbeService {

  protected def sampleStore: otg.sparql.OTGSamples = context.samples

  var chembl: ChEMBL = _
  var drugBank: DrugBank = _
  var homologene: B2RHomologene = _
  var targetmine: Option[IntermineConnector] = None

  override def localInit(c: Configuration) {
    super.localInit(c)
    chembl = new ChEMBL()
    drugBank = new DrugBank()
    homologene = new B2RHomologene()

    val mines = new Intermines(c.intermineInstances)
    mines.byTitle.get("TargetMine") match {
      case Some(tg) =>
        targetmine = Some(new IntermineConnector(tg, platformsCache))
      case None =>
    }
  }

  private def probeStore: OTGProbes = context.probes

  override protected def reloadAppInfo = {
    val r = new AppInfoLoader(probeStore, configuration, baseConfig, appName).load
    r.setPredefinedGroups(predefinedGroups)
    r
  }

  @throws[TimeoutException]
  override def goTerms(pattern: String): Array[String] =
    probeStore.goTerms(pattern).map(_.name).toArray

  @throws[TimeoutException]
  override def probesForGoTerm(goTerm: String): Array[String] = {
    val pmap = context.matrix.probeMap
    probeStore.forGoTerm(GOTerm("", goTerm)).map(_.identifier).filter(pmap.isToken).toArray
  }

  @throws[TimeoutException]
  private def predefinedGroups: Array[Group] = {
    //we call this from localInit and sessionInfo.sampleFilter
    //will not be available yet

    val sf = SampleFilter(instanceURI = instanceURI)
    val r = sampleStore.sampleGroups(sf).filter(!_._2.isEmpty).map(x =>
      new Group(schema, x._1, x._2.map(x => asJavaSample(x)).toArray))
    r.toArray
  }

  //TODO consider removing the sc argument (and the need for sp in orthologs)
  @throws[TimeoutException]
  override def probesTargetedByCompound(sc: SampleClass, compound: String, service: String,
    homologous: Boolean): Array[String] = {
    val cmp = Compound.make(compound)
    val sp = asSpecies(sc)
    val proteins = service match {
      case "CHEMBL" => chembl.targetsFor(cmp)
      case "DrugBank" => drugBank.targetsFor(cmp)
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
    val pbs = if (homologous) {
      val oproteins = uniprot.orthologsFor(proteins, sp).values.flatten.toSet
      probeStore.forUniprots(oproteins ++ proteins)
      //      OTGOwlim.probesForEntrezGenes(genes)
    } else {
      probeStore.forUniprots(proteins)
    }
    val pmap = context.matrix.probeMap //TODO context.probes(filter)
    pbs.toSet.map((p: Probe) => p.identifier).filter(pmap.isToken).toArray
  }

  override def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] = {
    implicit val sf = getState.sampleFilter

    val mirnaSources = getOtherServiceState[NetworkState](NetworkState.stateKey)
      .map(_.mirnaSources).getOrElse(Array())

    new otgviewer.server.AssociationResolver(probeStore, sampleStore,
        b2rKegg, uniprot, chembl, drugBank,
        targetmine, mirnaSources,
        sc, types, _probes).resolve
  }
}
