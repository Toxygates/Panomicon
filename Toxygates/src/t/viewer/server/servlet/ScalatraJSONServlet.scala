package t.viewer.server.servlet

import java.text.SimpleDateFormat
import java.util.Date

import javax.servlet.ServletContext
import org.scalatra._
import t.common.shared.ValueType
import t.db.BasicExprValue
import t.model.sample.Attribute
import t.model.sample.CoreParameter.{ControlGroup, Platform, SampleId, Type}
import t.model.sample.OTGAttribute.{Compound, DoseLevel, ExposureTime, Organ, Organism, Repeat, TestType}
import t.sparql.{Batch, BatchStore, Dataset, DatasetStore, SampleClassFilter, SampleFilter}
import t.viewer.server.Configuration
import t.viewer.server.Conversions.asJavaSample
import t.viewer.server.matrix.{ExpressionRow, MatrixController, PageDecorator}
import t.viewer.shared.OTGSchema
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW, _}


package json {

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
}

object Encoders {
  // date should be added, either ISO 8601 or millis since 1970
  // https://stackoverflow.com/a/15952652/689356
  //Work in progress - this supposedly conforms to ISO 8601
  val jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dtRW = readwriter[String].bimap[Date](
    output => jsonDateFormat.format(output),
    input =>
      try {
        jsonDateFormat.parse(input)
      } catch {
        case _: Exception => new Date(0)
      }
  )

  implicit val bevRw: RW[BasicExprValue] = macroRW
  implicit val erRw: RW[ExpressionRow] = macroRW
  implicit val dsRW: RW[Dataset] = macroRW
  implicit val batRW: RW[Batch] = macroRW
}

class ScalatraJSONServlet(scontext: ServletContext) extends ScalatraServlet with MinimalTServlet {
  import Encoders._

  val tconfig = Configuration.fromServletContext(scontext)
  var sampleFilter: SampleFilter = SampleFilter(tconfig.instanceURI)

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
    val exists = batchStore.list().contains(requestedBatchId)
    println("Here are the batch ids")
    println(batchStore.list())
    if (!exists) halt(400)

    val batchURI = BatchStore.packURI(requestedBatchId)
    val sf = SampleFilter(tconfig.instanceURI, Some(batchURI))
    val data = sampleStore.sampleAttributeValueQuery(overviewParameters.map(_.id))(sf)()
    write(data)
  }

  get("/samples") {
    val scf = SampleClassFilter(
      Map.empty ++ params.iterator.flatMap(x => {
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

    val samples = sampleStore.sampleQuery(scf, sampleFilter)().map(s =>
      json.Sample(s.sampleId, s.sampleClass(Type), s.sampleClass(Platform)))

    params.get("limit") match {
      case Some(l) =>
        write(samples take l.toInt)
      case None =>
        write(samples)
    }
  }

  get("/parameterValues/:param") {
    val requestedParam = params("param")
    val attr = Option(baseConfig.attributes.byId(requestedParam)).
      getOrElse(halt(400))
    val values = sampleStore.attributeValues(SampleClassFilter().filterAll,
      attr, sampleFilter).toArray
    write(values)
  }

  post("/matrix") {
    //TODO: URL design for this request as a GET
    val matParams: json.MatrixParams = read[json.MatrixParams](request.body)
    println(s"Load request: $matParams")
    val valueType = ValueType.valueOf(matParams.valueType)

    val samples = matParams.groups.flatMap(_.samples.map(_.id))
    val fullSamples = Map.empty ++
      context.sampleStore.withRequiredAttributes(SampleClassFilter(), sampleFilter, samples)().map(
        s => (s.sampleId -> asJavaSample(s)))

    val schema = new OTGSchema()
    val groups = matParams.groups.map(g =>
      new t.common.shared.sample.Group(schema, g.name, g.samples.map(s => fullSamples(s.id)).toArray)
    )
    val controller = MatrixController(context, groups, matParams.initProbes, valueType)

    val pages = new PageDecorator(context, controller)
    val defaultLimit = 100
    val page = matParams.limit match {
      case Some(l) => pages.getPageView(matParams.offset, l, true)
      case None => pages.getPageView(matParams.offset, defaultLimit, true)
    }
    write(page)
  }
}
