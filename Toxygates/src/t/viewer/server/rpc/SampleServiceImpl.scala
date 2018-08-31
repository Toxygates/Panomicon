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

package t.viewer.server.rpc

import java.util.{ List => JList }

import scala.collection.JavaConverters._
import t.common.server.GWTUtils._

import otgviewer.shared.Pathology
import t.common.server.ScalaUtils
import t.common.shared._
import t.common.shared.sample._
import t.common.shared.sample.search.MatchCondition
import t.model.SampleClass
import t.model.sample.Attribute
import t.model.sample.SampleLike
import t.platform.Probe
import t.sparql._
import t.sparql.secondary._
import t.viewer.client.rpc._
import t.viewer.server._
import t.viewer.server.CSVHelper.CSVFile
import t.viewer.server.Conversions._
import t.viewer.shared._

object SampleServiceImpl {
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

class SampleState(instanceURI: Option[String]) {
  var sampleFilter: SampleFilter = SampleFilter(instanceURI = instanceURI)
}

/**
 * SPARQL query servlet.
 */
abstract class SampleServiceImpl extends StatefulServlet[SampleState] with
  SampleService {

  import SampleServiceImpl._
  import ScalaUtils._

  type DataColumn = t.common.shared.sample.DataColumn[Sample]

  var instanceURI: Option[String] = None

  private def sampleStore: Samples = context.samples

  protected var uniprot: Uniprot = _
  protected var configuration: Configuration = _

  private def probeStore: Probes = context.probes

  lazy val annotations = new Annotations(schema, baseConfig,
        new Units(schema, sampleStore))

  override def localInit(conf: Configuration) {
    super.localInit(conf)

    //TODO: we shouldn't have to do this more than once
    val platforms = new t.sparql.Platforms(baseConfig)
    platforms.populateAttributes(baseConfig.attributes)

    /* Note: if staticInit does not read platformsAndProbes, some sparql queries
     * fail on startup (probably due to a race condition). Reasons unclear at the moment.
     */
    staticInit(context)
    this.configuration = conf
    this.instanceURI = conf.instanceURI
  }

  protected def appInfo = {
    Option(getThreadLocalRequest.getSession.
      getAttribute(ProbeServiceImpl.APPINFO_KEY).asInstanceOf[AppInfo]).
      getOrElse(throw new NoSessionException("AppInfo not initialised"))
  }

  protected def stateKey = "sparql"
  protected def newState = {
    //Initialise the selected datasets by selecting all, except shared user data.
    val defaultVisible = appInfo.datasets.filter(ds =>
      Dataset.isInDefaultSelection(ds.getTitle))

    val s = new SampleState(instanceURI)
    s.sampleFilter = sampleFilterFor(defaultVisible, None)
    s
  }

  protected implicit def sf: SampleFilter = getState.sampleFilter

  /**
   * Generate a new user key, to be used when the client does not already have one.
   */
  protected def makeUserKey(): String = {
    val time = System.currentTimeMillis()
    val random = (Math.random * Int.MaxValue).toInt
    "%x%x".format(time, random)
  }

  private def sampleFilterFor(ds: Array[Dataset], base: Option[SampleFilter]) = {
     val dsTitles = ds.toList.map(_.getTitle)
     val URIs = dsTitles.map(Datasets.packURI(_))
     base match {
       case Some(b) => b.copy(datasetURIs = URIs)
       case None => SampleFilter(datasetURIs = URIs)
     }
  }

  def chooseDatasets(ds: Array[Dataset]): Array[t.model.SampleClass] = {
    println("Choose datasets: " + ds.map(_.getTitle).mkString(" "))
    getState.sampleFilter = sampleFilterFor(ds, Some(getState.sampleFilter))

    sampleStore.sampleClasses.map(x => new SampleClass(x.asGWT)).toArray
  }

  @throws[TimeoutException]
  def parameterValues(ds: Array[Dataset], sc: SampleClass,
      parameter: String): Array[String] = {
    //Get the parameters without changing the persistent datasets in getState
    val filter = sampleFilterFor(ds, Some(getState.sampleFilter))
    val attr = baseConfig.attributes.byId(parameter)
    sampleStore.attributeValues(SampleClassFilter(sc).filterAll, attr)(filter).
      filter(x => !schema.isControlValue(parameter, x)).toArray
  }

  @throws[TimeoutException]
  def parameterValues(sc: Array[SampleClass], parameter: String): Array[String] = {
    sc.flatMap(x => parameterValues(x, parameter)).distinct.toArray
  }

  @throws[TimeoutException]
  def parameterValues(sc: SampleClass, parameter: String): Array[String] = {
    val attr = baseConfig.attributes.byId(parameter)
    sampleStore.attributeValues(SampleClassFilter(sc).filterAll, attr).
      filter(x => !schema.isControlValue(parameter, x)).toArray
  }

  def samplesById(ids: Array[String]): Array[Sample] = {
    sampleStore.samples(SampleClassFilter(), "id",
        ids).map(asJavaSample(_)).toArray
  }

  def samplesById(ids: JList[Array[String]]): JList[Array[Sample]] =
    ids.asScala.map(samplesById(_)).asGWT

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
  def units(sc: SampleClass,
      param: String, paramValues: Array[String]): Array[Pair[Unit, Unit]] =
      new Units(schema, sampleStore).units(sc, param, paramValues)

  def units(scs: Array[SampleClass], param: String,
      paramValues: Array[String]): Array[Pair[Unit, Unit]] = {
    scs.flatMap(units(_, param, paramValues))
  }

//  //TODO this is not used currently
//  @throws[TimeoutException]
//  def probes(columns: Array[SampleColumn]): Array[String] = {
//    val samples = columns.flatMap(_.getSamples)
//    val metadata = new TriplestoreMetadata(sampleStore, context.config.attributes)
//    val usePlatforms = samples.flatMap(s => metadata.parameter(
//        t.db.Sample(s.id), "platform_id")
//        ).distinct
//    usePlatforms.toVector.flatMap(x => platforms(x)).map(_.identifier).toArray
//  }

  //TODO move to OTG
  @throws[TimeoutException]
  def pathologies(column: SampleColumn): Array[Pathology] = Array()

  @throws[TimeoutException]
  def annotations(barcode: Sample): Annotation = {
    val params = sampleStore.parameterQuery(barcode.id)
    annotations.fromAttributes(barcode, params)
  }

  @throws[TimeoutException]
  def annotations(samples: Array[Sample], attributes: Array[Attribute]): Array[Annotation] =
    annotations.forSamples(sampleStore, samples, attributes)

  @throws[TimeoutException]
  def annotations(column: HasSamples[Sample], importantOnly: Boolean = false): Array[Annotation] =
    annotations.forSamples(sampleStore, column.getSamples, importantOnly)

  //TODO bio-param timepoint handling
  @throws[TimeoutException]
  def prepareAnnotationCSVDownload(column: HasSamples[Sample]): String =
    annotations.prepareCSVDownload(sampleStore, column.getSamples,
      configuration.csvDirectory, configuration.csvUrlBase)

  import scala.collection.{ Map => CMap, Set => CSet }

  def sampleSearch(sc: SampleClass, cond: MatchCondition, maxResults: Int):
      RequestResult[Pair[Sample, Pair[Unit, Unit]]] = {

    val sampleSearch = t.common.server.sample.search.IndividualSearch(cond,
        sc, sampleStore, schema, baseConfig.attributes)
    val pairs = sampleSearch.pairedResults.take(maxResults).map {
      case (sample, (treated, control)) =>
        new Pair(sample, new Pair(treated, control))
    }.toArray
    new RequestResult(pairs.toArray, pairs.size)
  }

  def unitSearch(sc: SampleClass, cond: MatchCondition, maxResults: Int):
      RequestResult[Pair[Unit, Unit]] = {

    val unitSearch = t.common.server.sample.search.UnitSearch(cond,
        sc, sampleStore, schema, baseConfig.attributes)
    val pairs = unitSearch.pairedResults.take(maxResults).map {
      case (treated, control) =>
        new Pair(treated, control)
    }.toArray
    new RequestResult(pairs, pairs.size)
  }

  def prepareCSVDownload(samples: Array[SampleLike],
      attributes: Array[Attribute]): String = {

    val csvFile = new CSVFile{
      def colCount = attributes.size
    	def rowCount = samples.size + 1

      def apply(x: Int, y: Int) = if (y == 0) {
        attributes(x)
      } else {  //y > 0
        samples(y - 1).get(attributes(x))
      }
    }

    CSVHelper.writeCSV("toxygates", configuration.csvDirectory,
        configuration.csvUrlBase, csvFile)
  }

}
