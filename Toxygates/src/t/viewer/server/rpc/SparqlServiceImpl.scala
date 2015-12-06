/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import scala.Array.canBuildFrom
import scala.collection.JavaConversions._
import scala.collection.{Set => CSet}
import java.util.{List => JList}
import otg.Species.Human
import t.common.server.ScalaUtils
import t.common.server.ScalaUtils.gracefully
import otgviewer.server.rpc.Conversions
import otgviewer.server.rpc.Conversions.asJava
import otgviewer.server.rpc.Conversions.asJavaSample
import otgviewer.server.rpc.Conversions.convertPairs
import otgviewer.shared.Pathology
import otgviewer.shared.TimeoutException
import t.BaseConfig
import t.TriplestoreConfig
import t.common.shared.AType
import t.common.shared.Dataset
import t.common.shared.Pair
import t.common.shared.SampleClass
import t.common.shared.sample.Annotation
import t.common.shared.sample.HasSamples
import t.common.shared.sample.Sample
import t.db.DefaultBio
import t.platform.Probe
import t.sparql._
import t.sparql.Instances
import t.sparql.Probes
import t.sparql.TriplestoreMetadata
import t.sparql.secondary._
import t.viewer.client.rpc.SparqlService
import t.viewer.server.Configuration
import t.viewer.server.Conversions.asSpecies
import t.viewer.server.Conversions.scAsScala
import t.viewer.shared.AppInfo
import t.viewer.shared.Association
import t.common.shared.StringList
import otg.db.OTGParameterSet
import t.common.shared.sample.Sample
import t.viewer.server.SharedDatasets
import t.common.shared.sample.Unit
import t.common.shared.sample.SampleColumn
import t.common.shared.Platform
import t.viewer.server.SharedPlatforms
import t.common.shared.clustering.ProbeClustering
import t.common.shared.clustering.HierarchicalClustering

object SparqlServiceImpl {
  var inited = false

  //TODO update mechanism for this
  var platforms: Map[String, Iterable[String]] = _

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

  import Conversions._
  import SparqlServiceImpl._
  import t.viewer.server.Conversions._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[Sample]

  var instanceURI: Option[String] = None

  private def probeStore: Probes = context.probes
  private def sampleStore: Samples = context.samples

  protected var uniprot: Uniprot = _
  protected var b2rKegg: B2RKegg = _
  protected var _appInfo: AppInfo = _

  override def localInit(conf: Configuration) {
    super.localInit(conf)
    //TODO if staticInit does not read platformsAndProbes, some sparql queries
    //fail on startup in Toxygates (probably due to a race condition).
    //Figure out why.
    staticInit(context)

    val ts = baseConfig.triplestore.triplestore
    uniprot = new LocalUniprot(ts)
    b2rKegg = new B2RKegg(ts)

    if (conf.instanceName == null || conf.instanceName == "") {
      instanceURI = None
    } else {
      instanceURI = Some(Instances.defaultPrefix + "/" + conf.instanceName)
    }

    this.instanceURI = instanceURI

    _appInfo = new AppInfo(conf.instanceName, sDatasets(),
        sPlatforms(), predefProbeLists(), probeClusterings())
  }

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

  def appInfo: AppInfo = {
    getSessionData() //initialise this if needed
     //Initialise the selected datasets by selecting all.
    chooseDatasets(_appInfo.datasets)
   _appInfo
  }

  private def predefProbeLists() = {
    val ls = probeStore.probeLists(instanceURI).mapInnerValues(p => p.identifier)
    val sls = ls.map(x => new StringList("probes", x._1, x._2.toArray)).toList
    new java.util.LinkedList(seqAsJavaList(sls.sortBy(_.name)))
  }

  private def probeClusterings() = {
    val ls = probeStore.probeLists(instanceURI).mapInnerValues(p => p.identifier)
    val sls = ls.map(x => new StringList("probes", x._1, x._2.toArray)).toList
    val cls = sls.map { x => ProbeClustering.buildFrom(x) }
    
    new java.util.LinkedList(seqAsJavaList(cls))
  }

  private def sDatasets(): Array[Dataset] = {
    val ds = new Datasets(baseConfig.triplestore) with SharedDatasets
    (instanceURI match {
      case Some(u) => ds.sharedListForInstance(u)
      case None => ds.sharedList
    }).toArray
  }

  private def sPlatforms(): Array[Platform] = {
    val pf = new Platforms(baseConfig.triplestore) with SharedPlatforms
    pf.sharedList.toArray
  }

  def chooseDatasets(ds: Array[Dataset]): scala.Unit = {
    val dsTitles = ds.toList.map(_.getTitle)
    println("Choose datasets: " + dsTitles)
    getSessionData.sampleFilter = getSessionData.sampleFilter.copy(datasetURIs =
      dsTitles.map(Datasets.packURI(_)))
  }

  @throws[TimeoutException]
  def parameterValues(ds: Array[Dataset], sc: SampleClass,
      parameter: String): Array[String] = {
    val oldFilter = getSessionData.sampleFilter
    chooseDatasets(ds)
    val r = parameterValues(sc, parameter)
    getSessionData.sampleFilter = oldFilter
    r
  }

  @throws[TimeoutException]
  def parameterValues(sc: Array[SampleClass], parameter: String): Array[String] = {
    sc.flatMap(x => parameterValues(x, parameter)).distinct.toArray
  }

  @throws[TimeoutException]
  def parameterValues(sc: SampleClass, parameter: String): Array[String] = {
    sampleStore.attributeValues(scAsScala(sc).filterAllExcludeControl, parameter).
      filter(x => !schema.isMajorParamSharedControl(x)).toArray
  }

  def samplesById(ids: Array[String]): Array[Sample] =
    sampleStore.samples(Filter("", ""), "id",
        ids).map(asJavaSample(_)).toArray

  //TODO compound_name is a dummy parameter below
  @throws[TimeoutException]
  def samples(sc: SampleClass): Array[Sample] = {
    val ss = sampleStore.sampleQuery.constrain(scAsScala(sc).filterAll)()
    ss.map(asJavaSample).toArray
  }

  @throws[TimeoutException]
  def samples(sc: SampleClass, param: String,
      paramValues: Array[String]): Array[Sample] =
    sampleStore.samples(sc.filterAll, param, paramValues).map(asJavaSample(_)).toArray

  @throws[TimeoutException]
  def samples(scs: Array[SampleClass], param: String,
      paramValues: Array[String]): Array[Sample] =
        scs.flatMap(x => samples(x, param, paramValues)).distinct.toArray

  @throws[TimeoutException]
  def sampleClasses(): Array[SampleClass] = {
  sampleStore.sampleClasses.map(x =>
    new SampleClass(new java.util.HashMap(mapAsJavaMap(x)))
    ).toArray
  }

  @throws[TimeoutException]
  def units(sc: SampleClass,
      param: String, paramValues: Array[String]): Array[Pair[Unit, Unit]] = {

    val majorParam = schema.majorParameter()
    //Ensure shared control is always included, if possible
    val useParamValues = if (param == majorParam) {
      val allMajors =
        sampleStore.attributeValues(scAsScala(sc).filterAll, majorParam)
      val shared = allMajors.filter(schema.isMajorParamSharedControl(_))
      (shared.toSeq ++ paramValues.toSeq)
    } else {
      paramValues.toSeq
    }

    //TODO rethink how to use batch here
    val ss = sampleStore.samples(sc.filterAll, param, useParamValues).
        groupBy(x =>(
            x.sampleClass(schema.timeParameter()),
            x.sampleClass.get("control_group")))

    //For each unit of treated samples inside a control group, all
    //control samples in that group are assigned as control.
    var r = Vector[Pair[Unit, Unit]]()
    for (((t, cg), samples) <- ss;
        treatedControl = samples.partition(s => !schema.isSelectionControl(s.sampleClass) )) {
      val treatedUnits = treatedControl._1.map(asJavaSample).
          groupBy(_.sampleClass.asUnit(schema))

      val cus = treatedControl._2.map(asJavaSample)
      val cu = if (!cus.isEmpty) {
        new Unit(cus.head.sampleClass().asUnit(schema),
          cus.toArray)
      } else {
        new Unit(sc.asUnit(schema), Array())
      }

      r ++= treatedUnits.map(u => new Pair(
          new Unit(u._1, u._2.toArray), cu))
      if (!cu.getSamples().isEmpty) {
        r :+= new Pair(cu, null: Unit) //add this as a pseudo-treated unit by itself
      }
    }
    r.toArray
  }

  def units(scs: Array[SampleClass], param: String,
      paramValues: Array[String]): Array[Pair[Unit, Unit]] = {
    scs.flatMap(units(_, param, paramValues))
  }

  //TODO this is not used currently
  @throws[TimeoutException]
  def probes(columns: Array[SampleColumn]): Array[String] = {
    val samples = columns.flatMap(_.getSamples)
    val metadata = new TriplestoreMetadata(sampleStore)
    val usePlatforms = samples.map(s => metadata.parameter(
        t.db.Sample(s.id), "platform_id")
        ).distinct
    usePlatforms.toVector.flatMap(x => platforms(x)).toArray
  }

  //TODO move to OTG
  @throws[TimeoutException]
  def pathologies(column: SampleColumn): Array[Pathology] = Array()

  private def parametersToAnnotation(barcode: Sample,
      ps: Iterable[(t.db.SampleParameter, Option[String])]): Annotation = {
     val params = ps.map(x => {
      var p = (x._1.humanReadable, x._2.getOrElse("N/A"))
      p = (p._1, OTGParameterSet.postReadAdjustment(p))
      new Annotation.Entry(p._1, p._2, OTGParameterSet.isNumerical(p._1))
    }).toSeq
    new Annotation(barcode.id, new java.util.ArrayList(params))
  }

  @throws[TimeoutException]
  def annotations(barcode: Sample): Annotation = {
    val params = sampleStore.parameterQuery(barcode.id)
    parametersToAnnotation(barcode, params)
  }

  //TODO get these from schema, etc.
  @throws[TimeoutException]
  def annotations(column: HasSamples[Sample], importantOnly: Boolean = false): Array[Annotation] = {
    val keys = if (importantOnly) {
      baseConfig.sampleParameters.previewDisplay
    } else {
      List()
    }

    column.getSamples.map(x => {
      val ps = sampleStore.parameterQuery(x.id, keys)
      parametersToAnnotation(x, ps)
    })
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
    val result = prs.map(_.identifier).filter(pmap.isToken).toArray

    Option(samples) match {
      case Some(_) => filterProbesByGroup(result, samples)
      case None => result
    }
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

    val result = probeStore.forGoTerm(got).map(_.identifier).filter(pmap.isToken).toArray

    Option(samples) match {
      case Some(_) => filterProbesByGroup(result, samples)
      case None => result
    }
  }

  import scala.collection.{ Map => CMap, Set => CSet }

  //TODO refactor this; instead of gathering all column logic here,
  //implement each column separately in a way that incorporates
  //both presentation and lookup code

  @throws[TimeoutException]
  def associations(sc: SampleClass, types: Array[AType],
    _probes: Array[String]): Array[Association] =
    new AnnotationResolver(sc, types, _probes).resolve

  import Association._

  protected class AnnotationResolver(sc: SampleClass, types: Array[AType],
      _probes: Iterable[String]) {
    val aprobes = probeStore.withAttributes(_probes.map(Probe(_)))

    lazy val proteins = toBioMap(aprobes, (_: Probe).proteins)

    def associationLookup(at: AType, sc: SampleClass, probes: Iterable[Probe])
      (implicit sf: SampleFilter): BBMap =
      at match {
        // The type annotation :BBMap is needed on at least one (!) match pattern
        // to make the match statement compile. TODO: research this
        case _: AType.Uniprot.type   => proteins: BBMap
        case _: AType.GO.type        => probeStore.goTerms(probes)

        case _: AType.KEGG.type =>
          toBioMap(probes, (_: Probe).genes) combine
            b2rKegg.forGenes(probes.flatMap(_.genes))
        case _: AType.Enzymes.type =>
          val sp = asSpecies(sc)
          b2rKegg.enzymes(probes.flatMap(_.genes), sp)
        case _ => throw new Exception("Unexpected annotation type")
      }

    val emptyVal = CSet(DefaultBio("error", "(Timeout or error)"))
    val errorVals = Map() ++ aprobes.map(p => (Probe(p.identifier) -> emptyVal))

    def queryOrEmpty[T](f: () => BBMap): BBMap = {
      gracefully(f, errorVals)
    }

    private def lookupFunction(t: AType)(implicit sf: SampleFilter): BBMap =
      queryOrEmpty(() => associationLookup(t, sc, aprobes))

    def standardMapping(m: BBMap): MMap[String, (String, String)] =
      m.mapKeys(_.identifier).mapInnerValues(p => (p.name, p.identifier))

    def resolve(): Array[Association] = {
      implicit val filt = sf
      val m1 = types.par.map(x => (x, standardMapping(lookupFunction(x)(filt)))).seq
      m1.map(p => new Association(p._1, convertPairs(p._2))).toArray
    }
  }

  @throws[TimeoutException]
  def geneSuggestions(sc: SampleClass, partialName: String): Array[String] = {
      val plat = for (scl <- Option(sc);
        org <- Option(scl.get("organism"));
        pl <- Option(schema.organismPlatform(org))) yield pl

      probeStore.probesForPartialSymbol(plat, partialName).map(_.identifier).toArray
  }

  def filterProbesByGroup(probes: Array[String], samples: JList[Sample]): Array[String] = {
    val platforms: Set[String] = samples.map(x => x.get("platform_id")).toSet
    val lookup = probeStore.platformsAndProbes
    val acceptProbes = platforms.flatMap(p => lookup(p))

    probes.filter(x => acceptProbes.contains(x))
  }

  def keywordSuggestions(partialName: String, maxSize: Int): Array[Pair[String, AType]] = {
    {
      b2rKegg.forPattern(partialName, maxSize).map(new Pair(_, AType.KEGG)) ++
      probeStore.goTerms(partialName, maxSize).map(x => new Pair(x.name, AType.GO))
    }.toArray
  }

}
