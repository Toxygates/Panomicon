/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otgviewer.server.rpc

import scala.Array.canBuildFrom

import otg.OTGContext
import t.platform.Species.Human
import otg.sparql._
import otgviewer.shared.Pathology
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
import t.viewer.shared.AppInfo
import t.viewer.shared.Association
import t.viewer.shared.TimeoutException

/**
 * This servlet is reponsible for making queries to RDF stores.
 * Future: May be split into a sample service and a probe service.
 */
class SparqlServiceImpl extends t.viewer.server.rpc.SparqlServiceImpl with OTGServiceServlet
  with otgviewer.client.rpc.SparqlService {

  private def probeStore: otg.sparql.Probes = context.probes
  private def sampleStore: otg.sparql.OTGSamples = context.samples

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

  override protected def refreshAppInfo(): AppInfo = {
    val r = super.refreshAppInfo()
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

  //TODO move to OTG
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

  @throws[TimeoutException]
  override def pathologies(column: SampleColumn): Array[Pathology] =
    column.getSamples.flatMap(x => sampleStore.pathologies(x.id)).map(
        otgviewer.server.rpc.Conversions.asJava(_))

  override def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] =
    new otgviewer.server.AssociationResolver(probeStore, sampleStore,
        b2rKegg, uniprot, chembl, drugBank,
        targetmine,
        sc, types, _probes).resolve

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
