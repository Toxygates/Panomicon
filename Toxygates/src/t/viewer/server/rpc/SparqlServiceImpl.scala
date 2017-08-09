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

package t.viewer.server.rpc

import java.util.{ List => JList }

import scala.Array.canBuildFrom
import scala.Vector
import scala.collection.JavaConversions._
import scala.collection.{ Set => CSet }

import SparqlServiceImpl.platforms
import javax.annotation.Nullable
import otg.db.OTGParameterSet
import otgviewer.shared.Pathology
import t.common.server.ScalaUtils
import t.common.shared.AType
import t.common.shared.Dataset
import t.common.shared.Pair
import t.common.shared.Platform
import t.model.SampleClass
import t.common.shared.StringList
import t.common.shared.clustering.ProbeClustering
import t.common.shared.sample.HasSamples
import t.common.shared.sample.NumericalBioParamValue
import t.common.shared.sample.Sample
import t.common.shared.sample.SampleColumn
import t.common.shared.sample.StringBioParamValue
import t.common.shared.sample.Unit
import t.db.DefaultBio
import t.platform.BioParameter
import t.platform.Probe
import t.sparql.BBMap
import t.sparql.Datasets
import t.sparql.MMap
import t.sparql.Platforms
import t.sparql.Probes
import t.sparql.SampleFilter
import t.sparql.Samples
import t.sparql.TriplestoreMetadata
import t.sparql.makeRich
import t.sparql.secondary.B2RKegg
import t.sparql.secondary.Gene
import t.sparql.secondary.GOTerm
import t.sparql.secondary.LocalUniprot
import t.sparql.secondary.Pathway
import t.sparql.secondary.Uniprot
import t.sparql.toBioMap
import t.util.PeriodicRefresh
import t.util.Refreshable
import t.viewer.client.rpc.SparqlService
import t.viewer.server.AssociationResolver
import t.viewer.server.CSVHelper
import t.viewer.server.CSVHelper.CSVFile
import t.viewer.server.Configuration

import t.viewer.server.Conversions._

import t.viewer.server.SharedDatasets
import t.viewer.server.SharedPlatforms
import t.viewer.shared.AppInfo
import t.viewer.shared.Association
import t.viewer.shared.TimeoutException
import t.common.shared.sample.Annotation
import t.platform.BioParameters
import t.viewer.server.Annotations

import t.common.shared.sample.search.MatchCondition
import t.common.shared.sample.BioParamValue
import t.sparql.SampleClassFilter
import t.model.shared.SampleClassHelper
import t.common.shared.sample.SampleClassUtils
import t.model.SampleClass
import t.viewer.server.Units
import t.common.shared.RequestResult

object SparqlServiceImpl {
  var inited = false

  //TODO update mechanism for this
  var platforms: Map[String, Iterable[Probe]] = _

  def staticInit(c: t.Context) = synchronized {
    if (!inited) {
      platforms = c.probes.platformsAndProbes
      inited = true
    }
  }
}

/**
 * SPARQL query servlet.
 */
abstract class SparqlServiceImpl extends TServiceServlet with SparqlService {

  import SparqlServiceImpl._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[Sample]

  var instanceURI: Option[String] = None

  private def probeStore: Probes = context.probes
  private def sampleStore: Samples = context.samples

  protected var uniprot: Uniprot = _
  protected var configuration: Configuration = _

  lazy val platformsCache = t.viewer.server.Platforms(probeStore)

  lazy val annotations = new Annotations(schema, baseConfig,
      new Units(schema, sampleStore))

  override def localInit(conf: Configuration) {
    super.localInit(conf)
    //TODO if staticInit does not read platformsAndProbes, some sparql queries
    //fail on startup in Toxygates (probably due to a race condition).
    //Figure out why.
    staticInit(context)
    this.configuration = conf

    val triplestore = baseConfig.triplestore.triplestore
    uniprot = new LocalUniprot(triplestore)

    this.instanceURI = conf.instanceURI
    //Preload appInfo
    appInfoLoader.latest
  }

  //AppInfo refreshes at most once per day.
  //This is to allow updates such as clusterings, annotation info etc to feed through.
  protected val appInfoLoader: Refreshable[AppInfo] =
    new PeriodicRefresh[AppInfo]("AppInfo", 3600 * 24) {
    def reload(): AppInfo = {
      refreshAppInfo()
    }
  }

  /**
   * Called when AppInfo needs a full refresh.
   */
  protected def refreshAppInfo(): AppInfo = {
    val probeLists = predefProbeLists()

    new AppInfo(configuration.instanceName, Array(),
        sPlatforms(), probeLists,
        configuration.intermineInstances.toArray,
        probeClusterings(probeLists), appName,
        makeUserKey(), getAnnotationInfo,
        annotations.allParamsAsShared.toArray)
  }

  protected lazy val b2rKegg: B2RKegg =
    new B2RKegg(baseConfig.triplestore.triplestore)

  protected class SparqlState(ds: Datasets) {
    var sampleFilter: SampleFilter = SampleFilter(instanceURI = instanceURI)
  }

  protected def getSessionData(): SparqlState = {
    val r = getThreadLocalRequest().getSession().getAttribute("sparql").
      asInstanceOf[SparqlState]
    if (r == null) {
      val ds = new Datasets(baseConfig.triplestore)
      val ss = new SparqlState(ds)
      setSessionData(ss)
      ss
    } else {
      r
    }
  }

  protected implicit def sf: SampleFilter = getSessionData.sampleFilter

  protected def setSessionData(m: SparqlState) =
    getThreadLocalRequest().getSession().setAttribute("sparql", m)

    /**
     * Obtain data sources information for AppInfo
     */
  protected def getAnnotationInfo: Array[Array[String]] = {
    val dynamic = probeStore.annotationsAndComments.toArray
    val static = staticAnnotationInfo
    Array(
      (dynamic ++ static).map(_._1),
      (dynamic ++ static).map(_._2))
  }

  protected def staticAnnotationInfo: Seq[(String, String)] = Seq()

  /**
   * Generate a new user key, to be used when the client does not already have one.
   */
  protected def makeUserKey(): String = {
    val time = System.currentTimeMillis()
    val random = (Math.random * Int.MaxValue).toInt
    "%x%x".format(time, random)
  }

  def appInfo(@Nullable userKey: String): AppInfo = {
    getSessionData() //initialise this if needed

    val appInfo = appInfoLoader.latest

    /*
     * Reload the datasets since they can change often (with user data, admin
     * operations etc.)
     */
    appInfo.setDatasets(sDatasets(userKey))

    val sess = getThreadLocalRequest.getSession
    import GeneSetServlet._

    /*
     * From GeneSetServlet
     */
    val importGenes = sess.getAttribute(IMPORT_SESSION_KEY)
    if (importGenes != null) {
      val igs = importGenes.asInstanceOf[Array[String]]
      appInfo.setImportedGenes(igs)
      //Import only once
      sess.removeAttribute(IMPORT_SESSION_KEY)
    } else {
      appInfo.setImportedGenes(null)
    }

    if (getSessionData().sampleFilter.datasetURIs.isEmpty) {
      //Initialise the selected datasets by selecting all, except shared user data.
      val defaultVisible = appInfo.datasets.filter(ds =>
        ! Dataset.isSharedDataset(ds.getTitle))
      chooseDatasets(defaultVisible)
    }
   appInfo
  }

  private def predefProbeLists() = {
    val ls = probeStore.probeLists(instanceURI).mapInnerValues(p => p.identifier)
    val sls = ls.map(x => new StringList(
        StringList.PROBES_LIST_TYPE, x._1, x._2.toArray)).toList
    new java.util.LinkedList(seqAsJavaList(sls.sortBy(_.name)))
  }

  //Currently identical to predef probe lists
  private def probeClusterings(probeLists: Iterable[StringList]) = {
    val cls = probeLists.map(x => ProbeClustering.buildFrom(x))
    new java.util.LinkedList(seqAsJavaList(cls.toSeq))
  }

  private def sDatasets(userKey: String): Array[Dataset] = {
    val datasets = new Datasets(baseConfig.triplestore) with SharedDatasets
    var r = (instanceURI match {
      case Some(u) => datasets.sharedListForInstance(u)
      case None => datasets.sharedList
    })

    r = r.filter(ds => Dataset.isDataVisible(ds.getTitle, userKey))
    r.toArray
  }

  private def sPlatforms(): Array[Platform] = {
    val platforms = new Platforms(baseConfig.triplestore) with SharedPlatforms
    platforms.sharedList.toArray
  }

  private def sampleFilterFor(ds: Array[Dataset]) = {
     val dsTitles = ds.toList.map(_.getTitle)
    getSessionData.sampleFilter.copy(datasetURIs = dsTitles.map(Datasets.packURI(_)))
  }

  def chooseDatasets(ds: Array[Dataset]): scala.Unit = {
    println("Choose datasets: " + ds.map(_.getTitle).mkString(" "))
    getSessionData.sampleFilter = sampleFilterFor(ds)
  }

  @throws[TimeoutException]
  def parameterValues(ds: Array[Dataset], sc: SampleClass,
      parameter: String): Array[String] = {
    //Get the parameters without changing the persistent datasets in getSessionData
    val filter = sampleFilterFor(ds)
    sampleStore.attributeValues(SampleClassFilter(sc).filterAll, parameter)(filter).
      filter(x => !schema.isControlValue(parameter, x)).toArray
  }

  @throws[TimeoutException]
  def parameterValues(sc: Array[SampleClass], parameter: String): Array[String] = {
    sc.flatMap(x => parameterValues(x, parameter)).distinct.toArray
  }

  @throws[TimeoutException]
  def parameterValues(sc: SampleClass, parameter: String): Array[String] = {
    sampleStore.attributeValues(SampleClassFilter(sc).filterAll, parameter).
      filter(x => !schema.isControlValue(parameter, x)).toArray
  }

  def samplesById(ids: Array[String]): Array[Sample] = {
    sampleStore.samples(SampleClassFilter(), "id",
        ids).map(asJavaSample(_)).toArray
  }

  def samplesById(ids: JList[Array[String]]): JList[Array[Sample]] =
    new java.util.ArrayList(ids.map(samplesById(_)))

  @throws[TimeoutException]
  def samples(sc: SampleClass): Array[Sample] = {
    val samples = sampleStore.sampleQuery(SampleClassFilter(sc))(sf)()
    samples.map(asJavaSample).toArray
  }

  @throws[TimeoutException]
  def samples(sc: SampleClass, param: String,
      paramValues: Array[String]): Array[Sample] =
    sampleStore.samples(SampleClassFilter(sc), param, paramValues).map(asJavaSample(_)).toArray

  @throws[TimeoutException]
  def samples(scs: Array[SampleClass], param: String,
      paramValues: Array[String]): Array[Sample] =
        scs.flatMap(x => samples(x, param, paramValues)).distinct.toArray

  @throws[TimeoutException]
  def sampleClasses(): Array[t.model.SampleClass] = {
  sampleStore.sampleClasses.map(x =>
    new SampleClass(new java.util.HashMap(x))
    ).toArray
  }

  @throws[TimeoutException]
  def units(sc: SampleClass,
      param: String, paramValues: Array[String]): Array[Pair[Unit, Unit]] =
      new Units(schema, sampleStore).units(sc, param, paramValues)

  def units(scs: Array[SampleClass], param: String,
      paramValues: Array[String]): Array[Pair[Unit, Unit]] = {
    scs.flatMap(units(_, param, paramValues))
  }

  //TODO this is not used currently
  @throws[TimeoutException]
  def probes(columns: Array[SampleColumn]): Array[String] = {
    val samples = columns.flatMap(_.getSamples)
    val metadata = new TriplestoreMetadata(sampleStore, context.config.attributes)
    val usePlatforms = samples.flatMap(s => metadata.parameter(
        t.db.Sample(s.id), "platform_id")
        ).distinct
    usePlatforms.toVector.flatMap(x => platforms(x)).map(_.identifier).toArray
  }

  //TODO move to OTG
  @throws[TimeoutException]
  def pathologies(column: SampleColumn): Array[Pathology] = Array()

  @throws[TimeoutException]
  def annotations(barcode: Sample): Annotation = {
    val params = sampleStore.parameterQuery(barcode.id)
    annotations.fromAttributes(barcode, params)
  }

  @throws[TimeoutException]
  def annotations(column: HasSamples[Sample], importantOnly: Boolean = false): Array[Annotation] = {
    annotations.forSamples(sampleStore, column.getSamples, importantOnly)
  }

  //TODO bio-param timepoint handling
  @throws[TimeoutException]
  def prepareAnnotationCSVDownload(column: HasSamples[Sample]): String = {
    annotations.prepareCSVDownload(sampleStore, column.getSamples,
        configuration.csvDirectory, configuration.csvUrlBase)
  }

  //TODO remove sc
  @throws[TimeoutException]
  def pathways(sc: SampleClass, pattern: String): Array[String] =
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
  //TODO remove sc
  @throws[TimeoutException]
  def probesForPathway(sc: SampleClass, pathway: String): Array[String] = {
    probesForPathway(sc, pathway, null)
  }

  @throws[TimeoutException]
  def probesForPathway(sc: SampleClass, pathway: String, samples: JList[Sample]): Array[String] = {
    val pw = Pathway(null, pathway)
    val prs = probeStore.forPathway(b2rKegg, pw)
    val pmap = context.matrix.probeMap //TODO

    val result = prs.map(_.identifier).filter(pmap.isToken)
    filterByGroup(result, samples).toArray
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

  //TODO move to OTG
  @throws[TimeoutException]
  def probesTargetedByCompound(sc: SampleClass, compound: String, service: String,
    homologous: Boolean): Array[String] = Array()

  //TODO move to OTG
  @throws[TimeoutException]
  def goTerms(pattern: String): Array[String] =
    probeStore.goTerms(pattern).map(_.name).toArray

  //TODO move to OTG
  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String): Array[String] = {
    probesForGoTerm(goTerm, null)
  }

  //TODO move to OTG
  @throws[TimeoutException]
  def probesForGoTerm(goTerm: String, samples: JList[Sample]): Array[String] = {
    val pmap = context.matrix.probeMap
    val got = GOTerm("", goTerm)

    val result = probeStore.forGoTerm(got).map(_.identifier).filter(pmap.isToken)
    filterByGroup(result, samples)
  }

  import scala.collection.{ Map => CMap, Set => CSet }

  //TODO refactor this; instead of gathering all column logic here,
  //implement each column separately in a way that incorporates
  //both presentation and lookup code

  @throws[TimeoutException]
  def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] =
    new AssociationResolver(probeStore, b2rKegg, sc, types, _probes).resolve

  @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {
      val plat = for (scl <- Option(sc);
        org <- Option(scl.get("organism"));
        pl <- Option(schema.organismPlatform(org))) yield pl

      probeStore.probesForPartialSymbol(plat, partialName).map(_.identifier).toArray
  }

  private def filterProbesByGroupInner(probes: Iterable[String], group: Iterable[Sample]) = {
    val platforms: Set[String] = group.map(x => x.get("platform_id")).toSet
    val lookup = probeStore.platformsAndProbes
    val acceptable = platforms.flatMap(p => lookup(p)).map(_.identifier)
    probes.filter(acceptable.contains)
  }

  private def filterByGroup(result: Iterable[String], samples: JList[Sample]) =
    Option(samples) match {
      case Some(ss) => filterProbesByGroupInner(result, ss).toArray
      case None => result.toArray
    }

  def filterProbesByGroup(probes: Array[String], samples: JList[Sample]): Array[String] = {
    filterProbesByGroupInner(probes, samples).toArray
  }

  def keywordSuggestions(partialName: String, maxSize: Int): Array[Pair[String, AType]] = {
    {
      b2rKegg.forPattern(partialName, maxSize).map(new Pair(_, AType.KEGG)) ++
      probeStore.goTerms(partialName, maxSize).map(x => new Pair(x.name, AType.GO))
    }.toArray
  }

  def sampleSearch(sc: SampleClass, cond: MatchCondition, maxResults: Int): RequestResult[Sample] = {
    val searchSpace = sampleStore.sampleQuery(SampleClassFilter(sc))(sf)()

    val ss = t.common.server.sample.search.IndividualSearch(sampleStore, cond, annotations,
        searchSpace.map(asJavaSample))
    val rs = ss.results
    println(s"Search results (displaying 20/${rs.size}:")
    for (s <- rs take 20) {
      println(s)
    }    

    new RequestResult((rs take maxResults).toArray, rs.size)
  }

  import t.model.sample.CoreParameter.{ControlGroup => ControlGroupParam}

  def unitSearch(sc: SampleClass, cond: MatchCondition, maxResults: Int): RequestResult[Pair[Unit, Unit]] = {
    val searchSpace = sampleStore.sampleQuery(SampleClassFilter(sc))(sf)()

    val javaSamples: java.util.Collection[Sample] = searchSpace.map(asJavaSample)
    val units = Unit.formUnits(schema, javaSamples)
    val unitHelper = new Units(schema, sampleStore)

    val groupedUnits = unitHelper.byControlGroup(units)
    
    val controlGroupMap = Map() ++ groupedUnits.flatMap { case (param, units) =>
      units.find(unitHelper.isControl(_)).map(param -> _)
    }

    val ss = t.common.server.sample.search.UnitSearch(sampleStore, cond, annotations,
        units)
    val rs = ss.results.filter(u => !unitHelper.isControl(u))
    for (s <- rs) {
      for (param <- appInfoLoader.latest.numericalParameters()) {
        s.averageAttribute(param.id)
      }
      for (param <- appInfoLoader.latest.stringParameters()) {
        s.concatenateAttribute(param.id)
      }      
    }
    println(s"Search results (displaying 20/${rs.size}:")
    for (u <- rs take 20) {      
      println(u)
    }    

    val pairs = (for (
      unit <- rs take maxResults;
      reprSample = unit.getSamples()(0);
      controlUnit <- controlGroupMap.get(unitHelper.controlGroupKey(unit));
      pair = new Pair(unit, controlUnit)
      ) yield pair).toArray
    
    new RequestResult(pairs, rs.size)
  }

  def prepareUnitCSVDownload(units: Array[Unit], parameterNames: Array[String]):
    String = {

    val csvFile = new CSVFile{
      def colCount = parameterNames.size
    	def rowCount = units.size + 1

      def apply(x: Int, y: Int) = if (y == 0) {
        parameterNames(x)
      } else {  //y > 0
        units(y - 1).get(parameterNames(x))
      }
    }

    CSVHelper.writeCSV("toxygates", configuration.csvDirectory,
        configuration.csvUrlBase, csvFile)
  }
}
