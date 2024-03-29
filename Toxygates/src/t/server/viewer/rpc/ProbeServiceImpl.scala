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

package t.server.viewer.rpc

import java.util.{List => JList}
import javax.annotation.Nullable
import t.shared.common._
import t.shared.common.sample.{Group, Sample}
import t.model.SampleClass
import t.model.sample.{CoreParameter, OTGAttribute}
import t.platform.{PlatformRegistry, Probe, Species}
import t.platform.mirna.TargetTable
import t.sparql.secondary._
import t.sparql.{ProbeStore, SampleFilter, SampleStore}
import t.util.{PeriodicRefresh, Refreshable}
import t.gwt.viewer.client.rpc.ProbeService
import t.server.viewer.Conversions.{asJavaSample, asSpecies}
import t.server.viewer.servlet.GeneSetServlet
import t.server.viewer.{Configuration, _}
import t.shared.viewer.{AppInfo, Association, TimeoutException}
import t.shared.viewer.TimeoutException

import scala.collection.JavaConverters._
import scala.collection.mutable

object ProbeServiceImpl {
  val APPINFO_KEY = "appInfo"
}

/**
 * Servlet for querying probe related information.
 */
class ProbeServiceImpl extends TServiceServlet with ProbeService {
  import ProbeServiceImpl._

  protected def sampleStore: SampleStore = context.sampleStore
  protected def probeStore: ProbeStore = context.probeStore
  protected var instanceURI: Option[String] = None

  protected var uniprot: Uniprot = _
  protected lazy val b2rKegg: B2RKegg = new B2RKegg(baseConfig.triplestoreConfig.triplestore)
  protected lazy val associationLookup = new AssociationMasterLookup(context, defaultSampleFilter)

  protected var configuration: Configuration = _

  lazy val chembl: ChEMBL = new ChEMBL
  lazy val drugBank: DrugBank = new DrugBank

  //AppInfo refreshes at most once per day in a given instance of the ProbeServiceImpl.
  //This is to allow updates such as clusterings, annotation info etc to feed through.
  protected val appInfoLoader: Refreshable[AppInfo] =
    new PeriodicRefresh[AppInfo]("AppInfo", 3600 * 24) {
      def reload(): AppInfo = {
        reloadAppInfo
      }
    }

  override def localInit(conf: Configuration) {
    super.localInit(conf)
    this.configuration = conf
    this.instanceURI = conf.instanceURI

    val triplestore = baseConfig.triplestoreConfig.triplestore
    uniprot = new LocalUniprot(triplestore)
    //Preload
    appInfoLoader.latest

    context.platformRegistry //force preloading all platforms
  }

  protected def reloadAppInfo = {
    val r = new AppInfoLoader(probeStore, configuration, baseConfig).load
    r.setPredefinedGroups(predefinedGroups)
    r
  }

  protected def defaultSampleFilter = SampleFilter(instanceURI = instanceURI)

  /**
   * Get the latest AppInfo and adjust it w.r.t. the given user key.
   * Care must be taken not to expose sensitive data.
   * This call must be made before any other call to ProbeServiceImpl or SampleServiceImpl.
   */
  def appInfo(@Nullable userKey: String): AppInfo = {
    val appInfo = appInfoLoader.latest

    val sess = getThreadLocalRequest.getSession

    /*
     * From GeneSetServlet
     */
    val importGenes = sess.getAttribute(GeneSetServlet.IMPORT_SESSION_KEY)
    if (importGenes != null) {
      val igs = importGenes.asInstanceOf[Array[String]]
      appInfo.setImportedGenes(igs)
      //Import only once
      sess.removeAttribute(GeneSetServlet.IMPORT_SESSION_KEY)
    } else {
      appInfo.setImportedGenes(null)
    }

    //Keep this up-to-date also in the session
    setSessionAttr(APPINFO_KEY, appInfo)

    appInfo
  }

  @throws[TimeoutException]
  def geneSyms(_probes: Array[String]): Array[Array[String]] = {
    //Don't look up more than 500 probes
    val (lookup, nonLookup) = _probes.splitAt(500)
    val ps = lookup.map(p => Probe(p))
    val attrib = probeStore.withAttributes(ps)
    val r = lookup.map(pi => attrib.find(_.identifier == pi).
      map(_.symbolStrings.toArray).getOrElse(Array()))
    r ++ nonLookup.map(x => Array[String]())
  }

  @throws[TimeoutException]
  def probesForPathway(pathway: String): Array[String] = {
    probesForPathway(pathway, null)
  }

  @throws[TimeoutException]
  def probesForPathway(pathway: String, samples: JList[Sample]): Array[String] = {
    val pw = Pathway(null, pathway)
    val prs = probeStore.forPathway(b2rKegg, pw)
    val pmap = context.matrix.probeMap

    val result = prs.map(_.identifier).filter(pmap.isToken)
    filterByGroup(result, samples).toArray
  }

  def keywordSuggestions(partialName: String, maxSize: Int): Array[Pair[String, AType]] = {
    {
      b2rKegg.forPattern(partialName, maxSize).map(new Pair(_, AType.KEGG)) ++
        probeStore.goTerms(partialName, maxSize).map(x => new Pair(x.name, AType.GO))
    }.toArray
  }

  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String, samples: JList[Sample]): Array[String] = {
    val group = samples.asScala
    val platforms = group.map(x => x.get(CoreParameter.Platform)).toList.distinct
    val pmap = context.matrix.probeMap
    val got = GOTerm("", goTerm)

    val result = probeStore.forGoTerm(got, platforms).map(_.identifier).filter(pmap.isToken)
    filterByGroup(result, samples)
  }

  //Task: could use an enum rather than so many boolean parameters
  def identifiersToProbes(identifiers: Array[String], precise: Boolean,
                          quick: Boolean, titlePatternMatch: Boolean,
                          samples: JList[Sample]): Array[String] = {

    def geneLookup(gene: String): Option[Iterable[Probe]] =
      context.platformRegistry.geneLookup.get(Gene(gene))

    val ps = if (titlePatternMatch) {
      probeStore.forTitlePatterns(identifiers)
    } else {
      //Try to do a gene identifier based lookup first

      var geneMatch = Set[String]()
      var matchingGenes = Seq[Probe]()
      for (
        i <- identifiers; ps <- geneLookup(i)
      ) {
        geneMatch += i
        matchingGenes ++= ps
      }

      val nonGeneMatch = (identifiers.toSet -- geneMatch).toArray

      //Resolve remaining identifiers
      matchingGenes ++ probeStore.identifiersToProbes(context.matrix.probeMap,
        nonGeneMatch, precise, quick)
    }
    val result = ps.map(_.identifier).toArray

    filterByGroup(result, samples)
  }

  private def filterProbesByGroupInner(probes: Iterable[String], group: Iterable[Sample]) = {
    val platforms = group.map(x => x.get(CoreParameter.Platform)).toList.distinct
    val probeSet = platforms.flatMap(probeStore.probesForPlatform(_).map(_.identifier)).to[mutable.Set]
    probes.filter(probeSet.contains)
  }

  private def filterByGroup(result: Iterable[String], samples: JList[Sample]) =
    Option(samples) match {
      case Some(ss) => filterProbesByGroupInner(result, ss.asScala).toArray
      case None     => result.toArray
    }

   @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[Pair[String, String]] = {
     if (sc != null) {
      for {
        org <- Array(sc.get(OTGAttribute.Organism))
        sp = Species.withName(org)
        pl <- sp.platformsForProbeSuggestion
        hit <- probeStore.probesForPartialSymbol(Some(pl), partialName)
        suggest = new Pair(hit._1, hit._2)
      } yield suggest
     } else {
       probeStore.probesForPartialSymbol(None, partialName).
         map(hit => new Pair(hit._1, hit._2)).toArray
     }
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

  override def associations(sc: SampleClass, types: Array[AType],
                            probes: Array[String], sizeLimit: Int): Array[Association] = {

    // If resolving mRNA-miRNA associations, obtain target table and side platform
    // in order to create a MirnaResolver
    if (types.contains(AType.MiRNA) || types.contains(AType.MRNA)) {
      val netState = getOtherServiceState[NetworkState](NetworkState.stateKey)
      val targetTable = netState.map(_.targetTable).getOrElse(TargetTable.empty)

      associationLookup.doLookupForNetwork(sc, types, probes, sizeLimit,
        netState.flatMap(_.networks.headOption.map(_._2)), targetTable)
    } else {
      associationLookup.doLookup(sc, types, probes, sizeLimit)
    }
  }
}
