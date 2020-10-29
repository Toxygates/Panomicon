package t.viewer.server.servlet

import scala.collection.JavaConverters._

import java.text.SimpleDateFormat
import java.util.Date

import javax.servlet.ServletContext
import org.scalatra._
import t.common.shared.{AType, ValueType}
import t.db.BasicExprValue
import t.model.sample.Attribute
import t.model.sample.CoreParameter.{ControlGroup, Platform, SampleId, Type}
import t.model.sample.OTGAttribute.{Compound, DoseLevel, ExposureTime, Organ, Organism, Repeat, TestType}
import t.sparql.{Batch, BatchStore, Dataset, DatasetStore, SampleClassFilter, SampleFilter}
import t.viewer.server.{AssociationMasterLookup, Configuration}
import t.viewer.server.Conversions.asJavaSample
import t.viewer.server.matrix.{ExpressionRow, MatrixController, PageDecorator}
import t.viewer.shared.{Association, ColumnFilter, FilterType, ManagedMatrixInfo, OTGSchema}
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW, _}


package json {

  import t.viewer.server.matrix.ManagedMatrix

  object Sample { implicit val rw: RW[Sample] = macroRW }
  case class Sample(id: String, `type`: String, platform: String)

  object Group { implicit val rw: RW[Group] = macroRW }
  case class Group(name: String, sampleIds: Seq[String])

  object ColSpec { implicit val rw: RW[ColSpec] = macroRW }
  case class ColSpec(id: String, `type`: String = null)

  object FilterSpec { implicit val rw: RW[FilterSpec] = macroRW }
  case class FilterSpec(column: ColSpec, `type`: String, threshold: Double)

  object SortSpec { implicit val rw: RW[SortSpec] = macroRW }
  case class SortSpec(column: ColSpec, order: String)

  object MatrixParams { implicit val rw: RW[MatrixParams] = macroRW }
  case class MatrixParams(groups: Seq[Group], initProbes: Seq[String] = Seq(),
                          filtering: Seq[FilterSpec] = Seq(),
                          sorting: SortSpec = null) {

    def applyFilters(mat: ManagedMatrix): Unit = {
      for (f <- filtering) {
        val col = f.column
        val idx = mat.info.findColumn(col.id, col.`type`)
        if (idx != -1) {
          //Filter types can be, e.g.: ">", "<", "|x| >", "|x| <"
          val filt = new ColumnFilter(f.threshold, FilterType.parse(f.`type`))
          println(s"Filter for column $idx: $filt")
          mat.setFilter(idx, filt)
        } else {
          Console.err.println(s"Unable to find column $col. Filtering will not apply to this column.")
        }
      }
    }

    def applySorting(mat: ManagedMatrix): Unit = {
      val info =
      Option(sorting) match {
        case Some(sort) =>
          val idx = mat.info.findColumn(sort.column.id, sort.column.`type`)
          if (idx != -1) {
            val asc = sort.order == "ascending"
            mat.sort(idx, asc)
          } else {
            Console.err.println(s"Unable to find column ${sort.column}. Sorting will not apply to this column.")
          }
        case None =>
      }
    }
  }
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
  lazy val associationLookup = new AssociationMasterLookup(probeStore, sampleStore, sampleFilter)

  tServletInit(tconfig)

  lazy val datasetStore = new DatasetStore(baseConfig.triplestore)
  def datasets = datasetStore.items(tconfig.instanceURI)
  lazy val batchStore = new BatchStore(baseConfig.triplestore)
  lazy val sampleStore = context.sampleStore
  lazy val probeStore =  context.probeStore

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

  get("/batch/dataset/:dataset") {
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

  get("/sample/batch/:batch") {
    val requestedBatchId = params("batch")

    val fullList = batchStore.list()
    val exists = fullList.contains(requestedBatchId)
    println("Here are the batch ids")
    println(fullList)
    if (!exists) halt(400)

    val batchURI = BatchStore.packURI(requestedBatchId)
    val sf = SampleFilter(tconfig.instanceURI, Some(batchURI))
    val data = sampleStore.sampleAttributeValueQuery(overviewParameters.map(_.id))(sf)()
    write(data)
  }

  get("/sample") {
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

  def columnInfo(info: ManagedMatrixInfo): Seq[Map[String, String]] = {
      (0 until info.numColumns()).map(i => {
        Map("name" -> info.columnName(i),
          "parent" -> info.parentColumnName(i),
          "shortName" -> info.shortColumnName(i),
          "hint" -> info.columnHint(i)
        )
    })
  }

  /*
  URL parameters: valueType, offset, limit
  other parameters in MatrixParams
  Example request:
  curl -H "Content-Type:application/json" -X POST  http://127.0.0.1:8888/json/matrix\?limit\=5 \
   --data '{"filtering": [{ "column": {"id": "a", "type": "P-value"}, "type": "x <", "threshold": "0.05" } ],
      "sorting": { "column": { "id": "a" }, "order": "descending" },
      "groups": [ { "name": "a", "sampleIds":
        [ "003017689013", "003017689014",  "003017689015",
        "003017688002", "003017688003", "003017688004"
      ] }
    ] }'
   */

  post("/matrix") {
    val matParams: json.MatrixParams = read[json.MatrixParams](request.body)
    println(s"Load request: $matParams")
    val valueType = ValueType.valueOf(
      params.getOrElse("valueType", "Folds"))

    val sampleIds = matParams.groups.flatMap(_.sampleIds)
    val fullSamples = Map.empty ++
      context.sampleStore.withRequiredAttributes(SampleClassFilter(), sampleFilter, sampleIds)().map(
        s => (s.sampleId -> asJavaSample(s)))

    val schema = new OTGSchema()
    val groups = matParams.groups.map(g =>
      new t.common.shared.sample.Group(schema, g.name, g.sampleIds.map(s => fullSamples(s)).toArray)
    )

    val controller = MatrixController(context, groups, matParams.initProbes, valueType)
    matParams.applyFilters(controller.managedMatrix)
    matParams.applySorting(controller.managedMatrix)

    val pages = new PageDecorator(context, controller)
    val defaultLimit = 100

    val offset = params.getOrElse("offset", "0").toInt
    val page = params.get("limit") match {
      case Some(l) => pages.getPageView(offset, l.toInt, true)
      case None => pages.getPageView(offset, defaultLimit, true)
    }
    val ci = columnInfo(controller.managedMatrix.info)

    //writeJs avoids the problem of Map[String, Any] not having an encoder
    val r = Map("columns" -> writeJs(ci), "rows" -> writeJs(page))
    write(r)
  }

  def associationToJSON(a: Association): Seq[(String, Seq[(String, String)])] = {
    a.data.asScala.toSeq.map(x => {
      (x._1, //probe
        x._2.asScala.toSeq.map(v => {
          (v.formalIdentifier(), v.title())
        }))
    })
  }

  /**
   * Obtain association data for one association and a list of probes.
   * A representative sample must be included, from which the sample class is deduced when necessary.
   * Examle request: curl http://127.0.0.1:8888/json/association/GOBP/003017689013\?probes\=213646_x_at,213060_s_at
   */
  get("/association/:assoc/:sample") {
    val probes = params.getOrElse("probes", halt(400))
    try {
      val requestedType = AType.valueOf(params("assoc"))
      val reprSample = params("sample")
      println(s"Get AType $requestedType for $probes and representative sample $reprSample")
      val sampleData = context.sampleStore.withRequiredAttributes(SampleClassFilter(),
        sampleFilter, Seq(reprSample))().
        headOption.getOrElse(halt(400))
      println(sampleData.sampleClass)
      val limit = params.getOrElse("limit", "100").toInt

      val assoc = associationLookup.doLookup(sampleData.sampleClass, Array(requestedType),
        probes.split(","), limit).
        headOption.getOrElse(halt(400))

      write(associationToJSON(assoc))
    } catch {
      case iae: IllegalArgumentException =>
        System.err.println(s"Unknown association ${params("assoc")}")
        halt(400)
    }
  }
}
