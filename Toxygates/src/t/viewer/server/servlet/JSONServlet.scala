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

import scala.collection.JavaConverters._

import java.io.PrintWriter
import java.util.Date

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import t.sparql.{Datasets, SampleClassFilter, SampleFilter}
import t.viewer.server.SharedDatasets
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW}

package json {
  object Dataset {
    //Needed for upickle to convert this class to/from JSON
    implicit val rw: RW[Dataset] = macroRW
  }

  case class Dataset(id: String, title: String, numBatches: Int)
  object Sample { implicit val rw: RW[Sample] = macroRW }
  case class Sample(id: String, `type`: String, platform: String)
}

class JSONServlet extends HttpServlet with MinimalTServlet {

  var sampleFilter: SampleFilter = _

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    val conf = tServletInit(config)
    sampleFilter = SampleFilter(conf.instanceURI)
  }

  private def datasets =
    (new Datasets(baseConfig.triplestore) with SharedDatasets).sharedList.map(d => {
      json.Dataset(d.getId, d.getUserTitle, d.getNumBatches)
    }).toSeq

  lazy val sampleStore = context.sampleStore

  def isDataVisible(data: json.Dataset, userKey: String) = {
      data.id == t.common.shared.Dataset.userDatasetId(userKey) ||
        t.common.shared.Dataset.isSharedDataset(data.id) ||
        !data.id.startsWith("user-")
  }

  def getDatasets(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val reqId = Option(req.getParameter("id"))
    val userKey = Option(req.getParameter("userKey")).getOrElse("")

    val out = new PrintWriter(resp.getOutputStream)
    val data = reqId match {
      case Some(id) => datasets.filter(_.id == id)
      case _ => datasets.filter(isDataVisible(_, userKey))
    }

    //Write (from upickle) converts objects to a JSON string
    out.println(write(data))
    out.flush()
  }

  def getParameterValues(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val param = Option(req.getParameter("param")).getOrElse(throw new Exception("Please specify parameter"))
    val out = new PrintWriter(resp.getOutputStream)
    val attr = baseConfig.attributes.byId(param)
    val values = sampleStore.attributeValues(SampleClassFilter().filterAll,
      attr, sampleFilter).toArray

    out.println(write(values))
    out.flush()
  }

  def getSamples(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    import t.model.sample.CoreParameter._

    val body = req.getReader.lines.iterator.asScala.mkString("\n")

    //read uses upickle to decode a given type from JSON
    val constraints: Map[String, String] = read[Map[String, String]](body)

    val scf = SampleClassFilter(
      constraints.flatMap(x => {
        val attrib = Option(baseConfig.attributes.byId(x._1))
        attrib match {
          case Some(a) => Some(a -> x._2)
          case None =>
            Console.err.println(s"Unknown attribute in request: ${x._1}")
            None
        }
      })
    )
    println(s"Decoded: ${scf.constraints}")
    val limit = Option(req.getParameter("limit"))

    val out = new PrintWriter(resp.getOutputStream)
    val samples = sampleStore.sampleQuery(scf, sampleFilter)().map(s =>
      json.Sample(s.sampleId, s.sampleClass(Type), s.sampleClass(Platform)))

    limit match {
      case Some(l) =>
        out.println(write(samples take l.toInt))
      case None =>
        out.println(write(samples))
    }
    out.flush()
  }

  def getTime(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val out = new PrintWriter(resp.getOutputStream)
    out.println("Invalid request. Current time:")
    out.println(new Date())
    out.flush()
  }

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    Option(req.getPathInfo) match {
      case Some("/datasets") => getDatasets(req, resp)
      case Some("/parameter") => getParameterValues(req, resp)
      case _ => getTime(req, resp)
    }
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    Option(req.getPathInfo) match {
      case Some("/samples") => getSamples(req, resp)
      case _ => getTime(req, resp)
    }
  }
}
  object Dataset {
    //Needed for upickle to convert this class to/from JSON
    implicit val rw: RW[Dataset] = macroRW
  }

  case class Dataset(id: String, title: String, numBatches: Int)
  object Sample { implicit val rw: RW[Sample] = macroRW }

case class Sample(id: String, `type`: String, platform: String)
