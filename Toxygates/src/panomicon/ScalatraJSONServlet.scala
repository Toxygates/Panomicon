package panomicon

import io.fusionauth.client.FusionAuthClient
import io.fusionauth.jwt.JWTDecoder
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.hmac.HMACVerifier
import io.fusionauth.jwt.rsa.RSAVerifier
import org.scalatra._
import org.scalatra.servlet.FileUploadSupport
import t.db.{BasicExprValue, Sample}
import t.global.KCDBRegistry
import t.manager.{BatchManager, Task, TaskRunner}
import t.model.sample.CoreParameter._
import t.model.sample.OTGAttribute._
import t.model.sample.{Attribute, CoreParameter}
import t.platform.mirna.TargetTableBuilder
import t.server.viewer.Conversions.asJavaSample
import t.server.viewer.matrix.{ExpressionRow, ManagedMatrix, MatrixController, PageDecorator}
import t.server.viewer.rpc.NetworkLoader
import t.server.viewer.servlet.MinimalTServlet
import t.server.viewer.{AssociationMasterLookup, Configuration, PlatformRegistry}
import t.shared.common.{AType, ValueType}
import t.shared.viewer._
import t.shared.viewer.mirna.MirnaSource
import t.shared.viewer.network.Interaction
import t.sparql.{Batch, BatchStore, Dataset, DatasetStore, PlatformStore, SampleClassFilter, SampleFilter}
import t.util.LRUCache
import ujson.Value
import upickle.default.{macroRW, ReadWriter => RW, _}

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}
import java.text.SimpleDateFormat
import java.util
import java.util.{Base64, Date}
import javax.servlet.ServletContext
import scala.collection.JavaConverters._

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
class ScalatraJSONServlet(scontext: ServletContext) extends ScalatraServlet
    with MinimalTServlet with FileUploadSupport {
  import Encoders._

  val tconfig = Configuration.fromServletContext(scontext)
  var sampleFilter: SampleFilter = SampleFilter(tconfig.instanceURI)
  lazy val associationLookup = new AssociationMasterLookup(probeStore, sampleStore, sampleFilter)

  tServletInit(tconfig)
  new PlatformStore(baseConfig).populateAttributes(baseConfig.attributes)

  lazy val datasetStore = new DatasetStore(baseConfig.triplestoreConfig)
  def datasets = datasetStore.getItems(tconfig.instanceURI)
  lazy val batchStore = new BatchStore(baseConfig.triplestoreConfig)
  lazy val sampleStore = context.sampleStore
  lazy val probeStore =  context.probeStore

  lazy val platformRegistry = new PlatformRegistry(probeStore)
  lazy val netLoader = new NetworkLoader(context, platformRegistry, baseConfig.data.mirnaDir)

  val authentication = new Authentication()

  error {
    //Catches exceptions during request processing.
    //In the future, we may add more specific error messages and responses here
    case e: Exception =>
      e.printStackTrace()
      halt(500)
  }

  def isDataVisible(data: Dataset, userKey: String) = {
    data.id == t.shared.common.Dataset.userDatasetId(userKey) ||
      t.shared.common.Dataset.isSharedDataset(data.id) ||
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

    val batchStore = new BatchStore(baseConfig.triplestoreConfig)
    val r = batchStore.items(tconfig.instanceURI, Some(requestedDatasetId))
    write(r)
  }

  protected def overviewParameters: Seq[Attribute] =
    Seq(SampleId, Type, Organism, TestType, Repeat, Organ, Compound, DoseLevel,
      ExposureTime, Platform, Treatment)

  get("/sample/batch/:batch") {
    val requestedBatchId = params("batch")

    val fullList = batchStore.getList()
    val exists = fullList.contains(requestedBatchId)
    println("Here are the batch ids")
    println(fullList)
    if (!exists) halt(400)

    val batchURI = BatchStore.packURI(requestedBatchId)
    val sf = SampleFilter(tconfig.instanceURI, Some(batchURI))
    val scf = SampleClassFilter()
    val samples = sampleStore.sampleQuery(scf, sf)().map(sampleToMap)
    write(samples)
  }

  def sampleToMap(s: Sample): collection.Map[String, String] = {
    s.sampleClass.getMap.asScala.map(x => (x._1.id -> x._2))
  }

  get("/sample/treatment/:treatment") {
    val sf = SampleFilter(tconfig.instanceURI, None)
    val data = sampleStore.sampleQuery(SampleClassFilter(Map(Treatment -> params("treatment"))), sf)()
    write(data.map(sampleToMap))
  }

  /**
   * This request allows arbitrary GET parameter attribute filters, e.g. ?doseLevel=High
   */
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

    val samples = sampleStore.sampleQuery(scf, sampleFilter)().map(sampleToMap)

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

  /**
   * Cache the most recently used matrices in memory
   */
  private val matrixCache = new LRUCache[(json.MatrixParams, ValueType), MatrixController](10)

  post("/matrix") {
    import MatrixHandling._
    val matParams: json.MatrixParams = read[json.MatrixParams](request.body)
    println(s"Load request: $matParams")
    val valueType = ValueType.valueOf(
      params.getOrElse("valueType", "Folds"))

    val key = (matParams, valueType)
    val controller = matrixCache.get(key) match {
      case Some(mat) => mat
      case _ =>
        val c = loadMatrix(matParams, valueType)
        matrixCache.insert(key, c)
        c
    }

    val matrix = controller.managedMatrix

    val pages = new PageDecorator(context, controller)
    val defaultPageSize = 100

    val offset = params.getOrElse("offset", "0").toInt
    val pageSize = params.get("limit") match {
      case Some(l) => l.toInt
      case None => defaultPageSize
    }

    val numPages = (matrix.info.numRows.toFloat / pageSize).ceil.toInt

    val page = pages.getPageView(offset, pageSize, true)
    val ci = columnInfo(matrix.info)

    //writeJs avoids the problem of Map[String, Any] not having an encoder
    write(Map(
      "columns" -> writeJs(ci),
      "sorting" -> writeJs(Map(
        "column" -> writeJs(matrix.sortColumn.getOrElse(-1)),
        "ascending" -> writeJs(matrix.sortAscending))
      ),
      "rows" -> writeJs(flattenRows(page, matrix.info)),
      "last_page" -> writeJs(numPages),
    ))
  }

  def interactionsToJson(ints: Iterable[Interaction]): Seq[Value] = {
    ints.toSeq.map(i => {
      writeJs(Map(
        "from" -> writeJs(i.from().id()),
        "to" -> writeJs(i.to().id()),
        "label" -> writeJs(i.label()),
        "weight" -> writeJs(i.weight().doubleValue())
      ))
    })
  }

  post("/network") {
    import MatrixHandling._

    import java.lang.{Double => JDouble}
    val netParams: json.NetworkParams = read[json.NetworkParams](request.body)
    println(s"Load request: $netParams")
    val valueType = ValueType.valueOf(
      params.getOrElse("valueType", "Folds"))
    val defaultLimit = 100
    val pageSize = params.get("limit") match {
      case Some(l) => l.toInt
      case None => defaultLimit
    }

    var r = new TargetTableBuilder
    val mirnaSource = new MirnaSource(netParams.associationSource, "", false,
      Option(netParams.associationLimit).map(x => JDouble.parseDouble(x): JDouble).getOrElse(null: JDouble),
      0, null, null, null)
    for (t <- netLoader.mirnaTargetTable(mirnaSource)) {
      r.addAll(t)
    }
    val targetTable = r.build

    println(s"Constructed target table of size ${targetTable.size}")
    if (targetTable.isEmpty) {
      println("Warning: the target table is empty, no networks can be constructed.")
    }

    val mainGroups = filledGroups(netParams.matrix1)
    val mainInitProbes = netParams.matrix1.probes
    val sideGroups = filledGroups(netParams.matrix2)
    val netController = netLoader.load(targetTable, mainGroups, mainInitProbes.toArray,
      sideGroups, valueType, pageSize)
    val network = netController.makeNetwork
    write(Map(
      "interactions" -> interactionsToJson(network.interactions().asScala)
    ))
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

  /**
   * Obtain attributes for a batch
   */
  get("/attribute/batch/:batch") {
    // we ignore this parameter for now because per-batch attributes
    // aren't implemented yet
    val batch = params("batch")

    val attributes = baseConfig.attributes.getAll()
    val values = attributes.asScala.map(attrib => writeJs(Map(
      "id" -> writeJs(attrib.id()),
      "title" -> writeJs(attrib.title()),
      "isNumerical" -> writeJs(attrib.isNumerical))))
    write(values)
  }

  /**
   * Obtain attribute values for a set of samples
   */
  post("/attributeValues") {
    val params = ujson.read(request.body)
    val sampleIds: Seq[String] = params.obj.get("samples").map(_.arr).getOrElse(List()).map(v => v.str)
    val batches: Seq[String] = params.obj.get("batches").map(_.arr).getOrElse(List()).map(v => v.str)
    val attributes: Seq[Attribute] = params("attributes").arr.map(v => baseConfig.attributes.byId(v.str))
    val samplesWithValues = sampleStore.sampleAttributeValues(sampleIds, batches, attributes)
    write(samplesWithValues.map(sampleToMap))
  }

  get("/login") {
    val (verifier, challenge) = authentication.generatePKCEPair()
    session("verifier") = verifier
    redirect(authentication.loginRedirectUri(challenge))
  }

  get("/oauth-redirect") {
    if (!session.contains("verifier")) halt(401, "Unauthenticated")

    val authorizationCode = params("code")
    val verifier: String = session("verifier").asInstanceOf[String]

    val tokenResponse = authentication
      .exchangeOAuthCodeForAccessToken(authorizationCode, verifier)

    if (tokenResponse.wasSuccessful()) {
      val responseContent = tokenResponse.successResponse
      val accessToken = responseContent.token
      val refreshToken = responseContent.refreshToken

      response.addHeader("Set-Cookie", s"__Host-jwt=$accessToken; Secure; Path=/; HttpOnly; SameSite=Strict")
      response.addHeader("Set-Cookie", s"__Host-refreshToken=$refreshToken; Secure; Path=/; HttpOnly; SameSite=Strict")

      redirect("http://localhost:4200/admin/")
    } else {
      if (tokenResponse.errorResponse != null) {
        tokenResponse.errorResponse.toString
      } else if (tokenResponse.exception != null) {
        tokenResponse.exception.toString
      } else {
        s"Failed; status = ${tokenResponse.status}"
      }
    }
  }

  def verifyJWT(): JWT = {
    authentication.getJwtToken(request.getCookies) match {
      case Left((token, tokenString)) => {
        response.addHeader("Set-Cookie", authentication.cookieHeader(tokenString))
        token
      }
      case Right(error) => halt(401)
    }
  }

  def verifyRole(role: String): JWT = {
    val jwt = verifyJWT()
    val roles = jwt.getList("roles").asInstanceOf[util.List[String]]
    if (!roles.contains(role)) halt(401) else jwt
  }

  get("/check-cookie") {
    verifyJWT()
  }

  get("/roles") {
    val jwt = verifyJWT()
    val roles = jwt.getList("roles").asInstanceOf[util.List[String]].asScala
    writeJs(roles)
  }

  post("/upload") {
    verifyRole("admin")
    val file = fileParams("fileKey")
    val fileContents = new String(file.get(), StandardCharsets.UTF_8);
    println(fileContents)
    fileContents
  }

  delete("/batch/:batch") {
    verifyRole("admin")
    val batchId = params("batch")
    val batchManager = new BatchManager(context)
    runTasks(batchManager.delete(batchId, false))
    "Deleting batch " + batchId
  }

  protected def runTasks(task: Task[_]) {
    if (!TaskRunner.available) {
      throw new Exception("Another task is already in progress.")
    }
//    setLastResults(None)
    val currentRequest = request
    val session = request.getSession
    TaskRunner.runThenFinally(task) {
      TaskRunner.log("Writing databases, this may take a while...")
      KCDBRegistry.closeWriters()
      TaskRunner.log("Databases written")
      TaskRunner.synchronized {
        try {
          val success = TaskRunner.errorCause == None
//          if (getAttribute[Option[OperationResults]]("lastResults", session).isEmpty) {
//            setAttribute("lastResults", Some(new OperationResults(
//              getAttribute[String]("lastTask", session), success, TaskRunner.resultMessages.toArray)), session)
//          }
//          if (success) {
//            maintenanceUploads(session).dropAll()
//          }
        } catch {
          case e: Exception =>
            println(e)
            e.printStackTrace()
        }
      }
    }
  }

  /**
   * Routines that support matrix loading requests
   */
  object MatrixHandling {
    def filledGroups(matParams: json.MatrixParams) = {
      val sampleIds = matParams.groups.flatMap(_.sampleIds)
      val fullSamples = Map.empty ++
        context.sampleStore.withRequiredAttributes(SampleClassFilter(), sampleFilter, sampleIds)().map(
          s => (s.sampleId -> s))
      matParams.groups.map(g => fillGroup(g.name, g.sampleIds.map(s => fullSamples(s))))
    }

    def loadMatrix(matParams: json.MatrixParams, valueType: ValueType): MatrixController = {
      val groups = filledGroups(matParams)
      val controller = MatrixController(context, groups, matParams.probes, valueType)
      val matrix = controller.managedMatrix
      matParams.applyFilters(matrix)
      matParams.applySorting(matrix)
      controller
    }

    def columnInfo(info: ManagedMatrixInfo): Seq[Map[String, Value]] = {
      (0 until info.numColumns()).map(i => {
        Map("name" -> writeJs(info.columnName(i)),
          "parent" -> writeJs(info.parentColumnName(i)),
          "shortName" -> writeJs(info.shortColumnName(i)),
          "hint" -> writeJs(info.columnHint(i)),
          "samples" -> writeJs(info.samples(i).map(s => s.id()))
        )
      })
    }

    import t.shared.common.sample.{Unit => TUnit}
    def unitForTreatment(sf: SampleFilter, treatment: String): Option[TUnit] = {
      val samples = sampleStore.sampleQuery(SampleClassFilter(Map(Treatment -> treatment)), sf)()
      if (samples.nonEmpty) {
        Some(new TUnit(samples.head.sampleClass, samples.map(asJavaSample).toArray))
      } else {
        None
      }
    }

    /**
     * By using the sample treatment ID, ensure that the group contains
     * all the available samples for a given treatment.
     * This is the default behaviour for /matrix requests for now; in the future, we may want to
     * make it optional, since the system in principle supports sub-treatment level sample groups.
     */
    def fillGroup(name: String, group: Seq[Sample]): t.shared.common.sample.Group = {
      if (group.isEmpty) {
        return new t.shared.common.sample.Group(name, Array[TUnit](), Array[TUnit]())
      }
      val batchURI = group.head.apply(CoreParameter.Batch)
      val sf = SampleFilter(tconfig.instanceURI, Some(batchURI))

      val treatedTreatments = group.map(s => s.sampleClass(Treatment)).distinct
      val controlTreatments = group.map(s => s.sampleClass(ControlTreatment)).distinct

      //Note: querying treated/control separately leads to one extra sparql query - can
      //probably be optimised away
      val treatedUnits = treatedTreatments.flatMap(t => unitForTreatment(sf, t))
      val controlUnits = controlTreatments.flatMap(t => unitForTreatment(sf, t))

      new t.shared.common.sample.Group(name, treatedUnits.toArray, controlUnits.toArray)
    }

    def flattenRows(rows: Seq[ExpressionRow], matrixInfo: ManagedMatrixInfo): Seq[Map[String, Value]] = {
      rows.map(r => Map(
        "probe" -> writeJs(r.probe),
        "probeTitles" -> writeJs(r.probeTitles),
        "geneIds" -> writeJs(r.geneIds.map(_.toInt)),
        "geneSymbols" -> writeJs(r.geneSymbols),
        "expression" -> writeJs(Map() ++ ((0 until matrixInfo.numColumns)
          .map(matrixInfo.columnName(_)) zip r
          .values.map(v => writeJs(v.value))))
      ))
    }
  }

}

  object Group { implicit val rw: RW[Group] = macroRW }
  case class Group(name: String, sampleIds: Seq[String])

  object FilterSpec { implicit val rw: RW[FilterSpec] = macroRW }
  case class FilterSpec(column: String, `type`: String, threshold: Double)

  object SortSpec { implicit val rw: RW[SortSpec] = macroRW }
  case class SortSpec(field: String, dir: String)

  object MatrixParams { implicit val rw: RW[MatrixParams] = macroRW }
  case class MatrixParams(groups: Seq[Group], probes: Seq[String] = Seq(),
                          filtering: Seq[FilterSpec] = Seq(),
                          sorter: SortSpec = null) {

    def applyFilters(mat: ManagedMatrix): Unit = {
      for (f <- filtering) {
        val col = f.column
        val idx = mat.info.findColumnByName(col)
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
      val defaultSortCol = 0
      val defaultSortAsc = false
      Option(sorter) match {
        case Some(sort) =>
          val idx = mat.info.findColumnByName(sort.field)
          if (idx != -1) {
            val asc = sort.dir == "asc"
            mat.sort(idx, asc)
          } else {
            Console.err.println(s"Unable to find column ${sort.field}. Sorting will not apply to this column.")
            mat.sort(defaultSortCol, defaultSortAsc)
          }
        case None =>
          mat.sort(defaultSortCol, defaultSortAsc)
      }
    }
  }

object NetworkParams { implicit val rw: RW[NetworkParams] = macroRW }

case class NetworkParams(matrix1: MatrixParams, matrix2: MatrixParams,
                           associationSource: String, associationLimit: String = null)
