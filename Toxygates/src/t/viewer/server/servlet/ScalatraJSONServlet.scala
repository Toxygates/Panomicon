package t.viewer.server.servlet

import javax.servlet.ServletContext
import org.scalatra._
import t.model.sample.Attribute
import t.model.sample.CoreParameter.{ControlGroup, Platform, SampleId, Type}
import t.model.sample.OTGAttribute.{Compound, DoseLevel, ExposureTime, Organ, Organism, Repeat, TestType}
import t.sparql.{BatchStore, Dataset, DatasetStore, SampleFilter}
import t.viewer.server.Configuration
import upickle.default.write
import t.viewer.server.servlet.Encoders._

class ScalatraJSONServlet(scontext: ServletContext) extends ScalatraServlet with MinimalTServlet {
  val tconfig = Configuration.fromServletContext(scontext)

  tServletInit(tconfig)

  private lazy val datasetStore = new DatasetStore(baseConfig.triplestore)
  private def datasets = datasetStore.items(tconfig.instanceURI)
  private lazy val batchStore = new BatchStore(baseConfig.triplestore)
  lazy val sampleStore = context.sampleStore

  get("instance") {
    <p>My instance is {tconfig.instanceName}</p>
  }

  def isDataVisible(data: Dataset, userKey: String) = {
    data.id == t.common.shared.Dataset.userDatasetId(userKey) ||
      t.common.shared.Dataset.isSharedDataset(data.id) ||
      !data.id.startsWith("user-")
  }

  get("/dataset") {
    val userKey = params.getOrElse("userDataKey", "")
    val data = datasets.filter(isDataVisible(_, userKey))
    write(data)
  }

  get("/dataset/:id") {
    val reqId = params("id")
    val data = datasets.find(_.id == reqId).getOrElse(halt(400))
    write(data)
  }

  get("/batches/dataset/:dataset") {
    val requestedDatasetId = params("dataset")
    val exists = datasetStore.list(tconfig.instanceURI).contains(requestedDatasetId)
    if (!exists) halt(400)

    val batchStore = new BatchStore(baseConfig.triplestore)
    val r = batchStore.items(tconfig.instanceURI, Some(requestedDatasetId))
    write(r)
  }

  protected def overviewParameters: Seq[Attribute] =
    Seq(SampleId, Type, Organism, TestType, Repeat, Organ, Compound, DoseLevel,
      ExposureTime, Platform, ControlGroup)

  get("/samples/batch/:batch") {
    val requestedBatchId = params("batch")

    // this currently does no checking based on the instance to see if a batch
    // should be accessible. So if someone guesses the id for a batch they shouldn't
    // have access to, they'll be able to access its samples
    val exists = batchStore.list().contains(requestedBatchId)
    println("Here are the batch ids")
    println(batchStore.list())
    if (!exists) halt(400)

    val batchURI = BatchStore.packURI(requestedBatchId)
    val sf = SampleFilter(None, Some(batchURI))
    val data = sampleStore.sampleAttributeValueQuery(overviewParameters.map(_.id))(sf)()
    write(data)
  }
}
