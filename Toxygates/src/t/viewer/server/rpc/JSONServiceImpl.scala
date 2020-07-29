package t.viewer.server.rpc

import java.util.Date

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import t.model.SampleClass
import t.sparql.{Datasets, SampleClassFilter, SampleFilter}
import t.viewer.server.{Configuration, SharedDatasets}
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW}

class JSONServiceImpl extends OTGServiceServlet {

  var sampleFilter: SampleFilter = _
  override def localInit(conf: Configuration): Unit = {
    super.localInit(conf)
    sampleFilter = SampleFilter(conf.instanceURI)
  }

  object Dataset {
    //Needed for upickle to convert this class to/from JSON
    implicit val rw: RW[Dataset] = macroRW
  }
  case class Dataset(id: String, title: String, numBatches: Int)

  private def datasets =
    (new Datasets(baseConfig.triplestore) with SharedDatasets).sharedList.map(d => {
      Dataset(d.getId, d.getUserTitle, d.getNumBatches)
    }).toSeq

  lazy val sampleStore = context.sampleStore

  def isDataVisible(data: Dataset, userKey: String) = {
      data.id == t.common.shared.Dataset.userDatasetId(userKey) ||
        t.common.shared.Dataset.isSharedDataset(data.id) ||
        !data.id.startsWith("user-")
  }

  def getDatasets(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val reqId = Option(req.getParameter("id"))
    val userKey = Option(req.getParameter("userKey")).getOrElse("")

    val out = resp.getWriter
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
    val out = resp.getWriter
    val attr = baseConfig.attributes.byId(param)
    val values = sampleStore.attributeValues(SampleClassFilter().filterAll,
      attr, sampleFilter).toArray

    out.println(write(values))
    out.flush()
  }

  def getTime(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val out = resp.getWriter
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
}
