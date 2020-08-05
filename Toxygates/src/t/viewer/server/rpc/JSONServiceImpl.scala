package t.viewer.server.rpc

import java.io.PrintWriter
import java.util.Date

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import t.model.SampleClass
import t.sparql.{Datasets, SampleClassFilter, SampleFilter}
import t.viewer.server.{Configuration, SharedDatasets}
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

class JSONServiceImpl extends HttpServlet with MinimalTServlet {

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

    val limit = Option(req.getParameter("limit"))
    val paramKey = Option(req.getParameter("paramKey"))
    val sc = paramKey match {
      case Some(k) =>
        val attrib = baseConfig.attributes.byId(k)
        val paramVal = req.getParameter("paramValue")
        SampleClassFilter(Map(attrib -> paramVal))
      case _ => SampleClassFilter()
    }

    val out = new PrintWriter(resp.getOutputStream)
    val samples = sampleStore.sampleQuery(sc, sampleFilter)().map(s =>
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
      case Some("/samples") => getSamples(req, resp)
      case _ => getTime(req, resp)
    }
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    Option(req.getPathInfo) match {
      case Some("/samples") => getSamples(req, resp)
      case _ => ???
    }
  }
}
