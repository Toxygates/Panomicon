package t.viewer.server.rpc

import java.util.{ List => JList }

import scala.collection.JavaConversions._

import javax.annotation.Nullable
import otg.model.sample.OTGAttribute
import t.BaseConfig
import t.common.server.ScalaUtils
import t.common.shared._
import t.common.shared.sample.Sample
import t.model.SampleClass
import t.model.sample.CoreParameter
import t.platform.Probe
import t.sparql.Datasets
import t.sparql.Probes
import t.sparql.SampleFilter
import t.sparql.secondary._
import t.util.PeriodicRefresh
import t.util.Refreshable

import t.viewer.client.rpc.ProbeService
import t.viewer.server._
import t.viewer.server.Configuration
import t.viewer.shared.AppInfo
import t.viewer.shared.TimeoutException
import t.viewer.shared.mirna.MirnaSource

class ProbeState {
  var mirnaSources: Array[MirnaSource] = Array()

  /*
   * Currently, the only use of this is in the association resolver,
   * which needs it for chembl/drugbank target lookup.
   */
  var sampleFilter: SampleFilter = _
}

object ProbeServiceImpl {
  val APPINFO_KEY = "appInfo"
}

abstract class ProbeServiceImpl extends StatefulServlet[ProbeState]
with ProbeService {
  import ProbeServiceImpl._
  import ScalaUtils._
  lazy val platformsCache = t.viewer.server.Platforms(probeStore)

  private def probeStore: Probes = context.probes
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

  protected def stateKey = "probes"
  protected def newState = new ProbeState

  protected def reloadAppInfo =
    new AppInfoLoader(probeStore, configuration, baseConfig,
      appName).load

  /**
   * "shared" datasets
   */
  private def sDatasets(userKey: String): Array[Dataset] = {
    val datasets = new Datasets(baseConfig.triplestore) with SharedDatasets
    var r = (instanceURI match {
      case Some(u) => datasets.sharedListForInstance(u)
      case None => datasets.sharedList
    })

    r = r.filter(ds => Dataset.isDataVisible(ds.getTitle, userKey))
    r.toArray
  }

  /**
   * Get the latest AppInfo and adjust it w.r.t. the given user key.
   * Care must be taken not to expose sensitive data.
   * This call must be made before any other call to ProbeServiceImpl or SampleServiceImpl.
   */
  def appInfo(@Nullable userKey: String): AppInfo = {
    getState.sampleFilter = SampleFilter(instanceURI = instanceURI)

    val appInfo = appInfoLoader.latest

    /*
     * Reload the datasets since they can change often (with user data, admin
     * operations etc.)
     */
    appInfo.setDatasets(sDatasets(userKey))

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
    getThreadLocalRequest.getSession.setAttribute(APPINFO_KEY, appInfo)

    appInfo
  }

  @throws[TimeoutException]
  def pathways(pattern: String): Array[String] =
    b2rKegg.forPattern(pattern).toArray

  //TODO: return a map instead
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

  //TODO more general two-way annotation resolution (don't hardcode a single annotation type
  //such as pathway)
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

  //TODO less boolean parameters, use an enum instead
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
      case Some(ss) => filterProbesByGroupInner(result, ss).toArray
      case None     => result.toArray
    }

  def filterProbesByGroup(probes: Array[String], samples: JList[Sample]): Array[String] = {
    filterProbesByGroupInner(probes, samples).toArray
  }

   @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {
      val plat = for (scl <- Option(sc);
        org <- Option(scl.get(OTGAttribute.Organism));
        pl <- Option(schema.organismPlatform(org))) yield pl

      probeStore.probesForPartialSymbol(plat, partialName).map(_.identifier).toArray
  }

  @throws[TimeoutException]
  def setMirnaSources(sources: Array[MirnaSource]): scala.Unit = {
    getState().mirnaSources = sources
  }

}
