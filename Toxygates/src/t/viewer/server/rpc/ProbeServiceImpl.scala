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

package t.viewer.server.rpc

import java.util.{List => JList}

import scala.collection.JavaConverters._
import javax.annotation.Nullable
import t.BaseConfig
import t.common.server.ScalaUtils
import t.common.shared._
import t.common.shared.sample.Sample
import t.model.SampleClass
import t.model.sample.{CoreParameter, OTGAttribute}
import t.platform.Probe
import t.sparql.Datasets
import t.sparql.ProbeStore
import t.sparql.SampleFilter
import t.sparql.secondary._
import t.util.PeriodicRefresh
import t.util.Refreshable
import t.viewer.client.rpc.ProbeService
import t.viewer.server._
import t.viewer.server.Conversions._
import t.common.server.GWTUtils._
import t.viewer.server.Configuration
import t.viewer.shared.AppInfo
import t.viewer.shared.TimeoutException
import t.viewer.shared.mirna.MirnaSource

object ProbeServiceImpl {
  val APPINFO_KEY = "appInfo"
}

/**
 * Servlet for querying probe related information.
 */
abstract class ProbeServiceImpl extends TServiceServlet with ProbeService {
  import ProbeServiceImpl._
  import ScalaUtils._
  lazy val platformsCache = t.viewer.server.Platforms(probeStore)

  private def probeStore: ProbeStore = context.probeStore
  protected var instanceURI: Option[String] = None

  protected var uniprot: Uniprot = _
  protected lazy val b2rKegg: B2RKegg =
    new B2RKegg(baseConfig.triplestore.triplestore)

  protected var configuration: Configuration = _

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

    val triplestore = baseConfig.triplestore.triplestore
    uniprot = new LocalUniprot(triplestore)
    //Preload
    appInfoLoader.latest
  }

  protected def reloadAppInfo =
    new AppInfoLoader(probeStore, configuration, baseConfig,
      appName).load

  private def sDatasets(userKey: String): Iterable[Dataset] = {
    val datasets = new Datasets(baseConfig.triplestore) with SharedDatasets
    var r = (instanceURI match {
      case Some(u) => datasets.sharedListForInstance(u)
      case None => datasets.sharedList
    })

    r.filter(ds => Dataset.isDataVisible(ds.getId, userKey))
  }

  protected def defaultSampleFilter = SampleFilter(instanceURI = instanceURI)

  /**
   * Get the latest AppInfo and adjust it w.r.t. the given user key.
   * Care must be taken not to expose sensitive data.
   * This call must be made before any other call to ProbeServiceImpl or SampleServiceImpl.
   */
  def appInfo(@Nullable userKey: String): AppInfo = {
    val appInfo = appInfoLoader.latest

    /*
     * Reload the datasets since they can change often (with user data, admin
     * operations etc.)
     */
    appInfo.setDatasets(sDatasets(userKey).toSeq.asGWT)

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
  def pathways(pattern: String): Array[String] =
    b2rKegg.forPattern(pattern).toArray

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
  def probesForGoTerm(goTerm: String): Array[String] = {
    probesForGoTerm(goTerm, null)
  }

  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String, samples: JList[Sample]): Array[String] = {
    val pmap = context.matrix.probeMap
    val got = GOTerm("", goTerm)

    val result = probeStore.forGoTerm(got).map(_.identifier).filter(pmap.isToken)
    filterByGroup(result, samples)
  }

  //Task: could use an enum rather than so many boolean parameters
  def identifiersToProbes(identifiers: Array[String], precise: Boolean,
                          quick: Boolean, titlePatternMatch: Boolean,
                          samples: JList[Sample]): Array[String] = {

    def geneLookup(gene: String): Option[Iterable[Probe]] =
      platformsCache.geneLookup.get(Gene(gene))

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
    val platforms: Set[String] = group.map(x => x.get(CoreParameter.Platform)).toSet
    val lookup = probeStore.platformsAndProbes
    val acceptable = platforms.flatMap(p => lookup(p)).map(_.identifier)
    probes.filter(acceptable.contains)
  }

  private def filterByGroup(result: Iterable[String], samples: JList[Sample]) =
    Option(samples) match {
      case Some(ss) => filterProbesByGroupInner(result, ss.asScala).toArray
      case None     => result.toArray
    }

  def filterProbesByGroup(probes: Array[String], samples: JList[Sample]): Array[String] = {
    filterProbesByGroupInner(probes, samples.asScala).toArray
  }

   @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[Pair[String, String]] = {
      val plat = for (scl <- Option(sc);
        org <- Option(scl.get(OTGAttribute.Organism));
        pl <- Option(schema.organismPlatform(org))) yield pl

      probeStore.probesForPartialSymbol(plat, partialName).map(x =>
        new Pair(x._1, x._2)).toArray
  }
}
