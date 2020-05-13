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

import t.sparql._
import otg.viewer.server.AppInfoLoader
import otg.viewer.server.DrugTargetResolver
import otg.viewer.server.MirnaResolver
import t.common.shared.AType
import t.common.shared.sample._
import t.model.SampleClass
import t.platform.Probe
import t.platform.mirna.TargetTable
import t.sparql._
import t.sparql.secondary._
import t.viewer.server.Configuration
import t.viewer.server.Conversions._
import t.viewer.server.rpc.NetworkState
import t.viewer.shared.Association
import t.viewer.shared.TimeoutException

class ProbeServiceImpl extends t.viewer.server.rpc.ProbeServiceImpl
  with OTGServiceServlet with otg.viewer.client.rpc.ProbeService {

  protected def sampleStore: SampleStore = context.sampleStore

  var chembl: ChEMBL = _
  var drugBank: DrugBank = _

  override def localInit(c: Configuration) {
    super.localInit(c)
    chembl = new ChEMBL()
    drugBank = new DrugBank()
  }

  private def probeStore: ProbeStore = context.probeStore

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

  //Task: try to remove the sc argument (and the need for sp in orthologs)
  @throws[TimeoutException]
  override def probesTargetedByCompound(sc: SampleClass, compound: String, service: String,
    homologous: Boolean): Array[String] = {
    val cmp = Compound.make(compound)
    val sp = asSpecies(sc).get
    val proteins = service match {
      case "CHEMBL" => chembl.targetsFor(cmp)
      case "DrugBank" => drugBank.targetsFor(cmp)
      case _ => throw new Exception("Unexpected probe target service request: " + service)
    }
    val pbs = if (homologous) {
      val oproteins = uniprot.orthologsFor(proteins, sp).values.flatten.toSet
      probeStore.forUniprots(oproteins ++ proteins)
      //      OTGProbes.probesForEntrezGenes(genes)
    } else {
      probeStore.forUniprots(proteins)
    }
    val pmap = context.matrix.probeMap // context.probes(filter)
    pbs.toSet.map((p: Probe) => p.identifier).filter(pmap.isToken).toArray
  }

  private lazy val drugTargetResolver = new DrugTargetResolver(sampleStore, chembl, drugBank).lookup

  override def associations(sc: SampleClass, types: Array[AType],
    probes: Array[String]): Array[Association] = {
    implicit val sf = defaultSampleFilter

    val netState = getOtherServiceState[NetworkState](NetworkState.stateKey)
    //    val mirnaSources = netState.map(_.mirnaSources).getOrElse(Array())
    val targetTable = netState.map(_.targetTable).getOrElse(TargetTable.empty)
    val sidePlatform = netState.flatMap(_.networks.headOption.map(_._2.sideMatrix.params.platform))

    val mirnaRes = new MirnaResolver(probeStore, platformsCache, targetTable, sidePlatform)
    val resolvers = Seq(drugTargetResolver,
      mirnaRes.lookup)
    val mainRes =  new otg.viewer.server.AssociationResolver(probeStore, sampleStore,
      b2rKegg)
    mirnaRes.limitState = mainRes.limitState

    mainRes.resolve(types, sc, sf, probes, resolvers)
  }
}
