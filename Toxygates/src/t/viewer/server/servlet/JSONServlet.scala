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

package t.viewer.server.servlet

import java.io.PrintWriter
import java.util.Date

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import t.common.shared.ValueType
import t.db.BasicExprValue
import t.sparql.{BatchStore, DatasetStore, SampleClassFilter, SampleFilter}
import t.viewer.server.Conversions._
import t.viewer.server.SharedDatasets
import t.viewer.server.matrix.{ExpressionRow, MatrixController, PageDecorator}
import t.viewer.shared.OTGSchema
import upickle.default.{macroRW, ReadWriter => RW, _}

import scala.collection.JavaConverters._


package json {
  object Dataset {
    //Needed for upickle to convert this class to/from JSON
    implicit val rw: RW[Dataset] = macroRW
  }
  case class Dataset(id: String, title: String, numBatches: Int)

  object Batch { implicit val rw: RW[Batch] = macroRW }
  case class Batch(id: String, comment: Option[String] = None, dataset: Option[String] = None)
  // date should be added, either ISO 8601 or millis since 1970
  // https://stackoverflow.com/a/15952652/689356
  // also, was using Options with default = None to implement optional parameters;
  // this works correctly by not serializing a field when its value is None, but
  // unfortunately Some(foo) gets serialized as an array
  // cf. https://github.com/lihaoyi/upickle/issues/75

  object Sample { implicit val rw: RW[Sample] = macroRW }
  case class Sample(id: String, `type`: String, platform: String)

  object Group { implicit val rw: RW[Group] = macroRW }
  case class Group(name: String, samples: Seq[Sample])

  object MatrixParams { implicit val rw: RW[MatrixParams] = macroRW }
  case class MatrixParams(groups: Seq[Group], valueType: String, offset: Int = 0, limit: Option[Int] = None,
                          initProbes: Seq[String] = Seq())

  object DataRow { implicit val rw: RW[DataRow] = macroRW }
  case class DataRow(probe: String, values: Seq[Double], calls: Seq[Char])
}

class JSONServlet extends HttpServlet with MinimalTServlet {
  implicit val bevRw: RW[BasicExprValue] = macroRW
  implicit val erRw: RW[ExpressionRow] = macroRW

  var sampleFilter: SampleFilter = _

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    val conf = tServletInit(config)
    sampleFilter = SampleFilter(conf.instanceURI)
  }

  private def datasets =
    (new DatasetStore(baseConfig.triplestore) with SharedDatasets).sharedList.map(d => {
      json.Dataset(d.getId, d.getUserTitle, d.getNumBatches)
    }).toSeq

  lazy val sampleStore = context.sampleStore

  def isDataVisible(data: json.Dataset, userKey: String) = {
      data.id == t.common.shared.Dataset.userDatasetId(userKey) ||
        t.common.shared.Dataset.isSharedDataset(data.id) ||
        !data.id.startsWith("user-")
  }

  def getDatasets(req: HttpServletRequest, out: PrintWriter): Unit = {
    val reqId = Option(req.getParameter("id"))
    val userKey = Option(req.getParameter("userKey")).getOrElse("")

    val data = reqId match {
      case Some(id) => datasets.filter(_.id == id)
      case _ => datasets.filter(isDataVisible(_, userKey))
    }

    //Write (from upickle) converts objects to a JSON string
    out.println(write(data))
  }

  def getBatchesForDataset(req: HttpServletRequest, out: PrintWriter): Unit = {
    val requestedDatasetId = Option(req.getParameter("id"))
    if (requestedDatasetId.isEmpty) {

      // should send an error status code, maybe 400?
      // also probably a JSON object with information on the error rather than this
      out.println("no id provided")

    } else {

      val datasetsMatchingRequest = datasets.filter(_.id == requestedDatasetId.get)
      if (datasetsMatchingRequest.isEmpty) {

        // same as above, but status code would probably be 404 here
        out.println("dataset not found")

      } else {

        // the following duplicates logic from BatchOpsImpl.getBatches
        // it's also inefficent since it requires three sparql queries
        val batchStore = new BatchStore(baseConfig.triplestore)
        val comments = batchStore.comments
        val datasets = batchStore.datasets
        val r = batchStore.list.flatMap(batchId => {
          val datasetForBatch = datasets(batchId)
          if (datasetForBatch == requestedDatasetId.get) {
            Some(json.Batch(batchId, Some(comments(batchId)), None))
          } else {
            None
          }
        })
        out.println(write(r))

      }

    }
  }

  def getParameterValues(req: HttpServletRequest, out: PrintWriter): Unit = {
    val param = Option(req.getParameter("param")).getOrElse(throw new Exception("Please specify parameter"))
    val attr = baseConfig.attributes.byId(param)
    val values = sampleStore.attributeValues(SampleClassFilter().filterAll,
      attr, sampleFilter).toArray

    out.println(write(values))
  }

  def getSamples(req: HttpServletRequest, body: String, out: PrintWriter): Unit = {
    import t.model.sample.CoreParameter._

    //read uses upickle to decode a given type from JSON
    val constraints: Map[String, String] = read[Map[String, String]](body)

    val scf = SampleClassFilter(
      constraints.flatMap(x => {
        val attrib = Option(baseConfig.attributes.byId(x._1))
        attrib match {
          case Some(a) => Some(a -> x._2)
          case None =>
            Console.err.println(s"Unknown attribute in request: ${x._1}. Ignoring!")
            None
        }
      })
    )
    println(s"Decoded: ${scf.constraints}")
    val limit = Option(req.getParameter("limit"))

    val samples = sampleStore.sampleQuery(scf, sampleFilter)().map(s =>
      json.Sample(s.sampleId, s.sampleClass(Type), s.sampleClass(Platform)))

    limit match {
      case Some(l) =>
        out.println(write(samples take l.toInt))
      case None =>
        out.println(write(samples))
    }
  }

  def getMatrix(req: HttpServletRequest, body: String, out: PrintWriter): Unit = {
    val params: json.MatrixParams = read[json.MatrixParams](body)
    println(s"Load request: $params")
    val valueType = ValueType.valueOf(params.valueType)

    val samples = params.groups.flatMap(_.samples.map(_.id))
    val fullSamples = Map.empty ++
      context.sampleStore.withRequiredAttributes(SampleClassFilter(), sampleFilter, samples)().map(
        s => (s.sampleId -> asJavaSample(s)))

    val schema = new OTGSchema()
    val groups = params.groups.map(g =>
      new t.common.shared.sample.Group(schema, g.name, g.samples.map(s => fullSamples(s.id)).toArray)
    )
    val controller = MatrixController(context, groups, params.initProbes, valueType)

    val pages = new PageDecorator(context, controller)
    val defaultLimit = 100
    val page = params.limit match {
      case Some(l) => pages.getPageView(params.offset, l, true)
      case None => pages.getPageView(params.offset, defaultLimit, true)
    }
    out.println(write(page))
  }

  def getTime(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val out = new PrintWriter(resp.getOutputStream)
    out.println("Invalid request. Current time:")
    out.println(new Date())
    out.flush()
    resp.sendError(418) //I'm a teapot
  }

  private def serveGet(req: HttpServletRequest, resp: HttpServletResponse,
                       handler: (HttpServletRequest, PrintWriter) => Unit) = {
    val out = new PrintWriter(resp.getOutputStream)
    try {
      handler(req, out)
    } finally {
      out.flush()
    }
  }

  private def servePost(req: HttpServletRequest, resp: HttpServletResponse,
                        handler: (HttpServletRequest, String, PrintWriter) => Unit) = {
    val out = new PrintWriter(resp.getOutputStream)
    val body = req.getReader.lines.iterator.asScala.mkString("\n")
    try {
      handler(req, body, out)
    } finally {
      out.flush()
    }
  }

  /**
   * Respond to a GET request
   * @param req
   * @param resp
   */
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    Option(req.getPathInfo) match {
      case Some("/datasets") => serveGet(req, resp, getDatasets)
      case Some("/batches") => serveGet(req, resp, getBatchesForDataset)
      case Some("/parameter") => serveGet(req, resp, getParameterValues)
      case _ => getTime(req, resp)
    }
  }

  /**
   * Respond to a POST request
   * @param req
   * @param resp
   */
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    Option(req.getPathInfo) match {
      case Some("/samples") => servePost(req, resp, getSamples)
      case Some("/matrix") => servePost(req, resp, getMatrix)
      case _ => getTime(req, resp)
    }
  }
}
