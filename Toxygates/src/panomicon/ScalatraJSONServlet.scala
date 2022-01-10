package panomicon

import io.fusionauth.jwt.domain.JWT
import org.scalatra._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import t.db.Sample
import t.model.sample.CoreParameter._
import t.model.sample.OTGAttribute._
import t.model.sample.{Attribute, CoreParameter}
import t.platform.{AffymetrixPlatform, BioPlatform, GeneralPlatform}
import t.server.viewer.servlet.MinimalTServlet
import t.server.viewer.Configuration
import t.shared.common.maintenance.{BatchUploadException, MaintenanceException}
import t.shared.common.{AType, ValueType}
import t.shared.viewer._
import t.sparql.{Batch, BatchStore, Dataset, DatasetStore, InstanceStore, PlatformStore, ProbeStore, SampleClassFilter, SampleFilter, TRDF}
import upickle.default._

import java.util
import javax.servlet.ServletContext
import scala.collection.JavaConverters._

class ScalatraJSONServlet(scontext: ServletContext) extends ScalatraServlet
    with MinimalTServlet with FileUploadSupport {
  import json.Encoders._

  //For file uploads
  configureMultipartHandling(MultipartConfig(
    maxFileSize = Some(512 * 1024 * 1024),
    fileSizeThreshold = Some(1024 * 1024))
  )

  val tconfig = Configuration.fromServletContext(scontext)
  var sampleFilter: SampleFilter = SampleFilter(tconfig.instanceURI)

  tServletInit(tconfig)
  new PlatformStore(baseConfig).populateAttributes(baseConfig.attributes)

  lazy val datasetStore = new DatasetStore(baseConfig.triplestoreConfig)
  lazy val batchStore = new BatchStore(baseConfig.triplestoreConfig)
  lazy val sampleStore = context.sampleStore
  lazy val probeStore =  context.probeStore

  val matrixHandling = new MatrixHandling(context, sampleFilter, tconfig)
  val networkHandling = new NetworkHandling(context, matrixHandling)
  val uploadHandling = new UploadHandling(context)

  val authentication = new Authentication()

  error {
    //Catches exceptions during request processing.
    //In the future, we may add more specific error messages and responses here
    case e: Exception =>
      e.printStackTrace()
      halt(500)
  }

  def paramOrHalt(key: String) = {
    try {
      params(key)
    } catch {
      case e: Exception =>
        halt(422, s"Missing parameter $key")
    }
  }

  def isDataVisible(data: Dataset, userKey: String) = {
    data.id == t.shared.common.Dataset.userDatasetId(userKey) ||
      t.shared.common.Dataset.isSharedDataset(data.id) ||
      !data.id.startsWith("user-")
  }

  get("/dataset") {
    contentType = "text/json"
    val userKey = params.getOrElse("userDataKey", "")
    val data = datasetStore.getItems(tconfig.instanceURI).
      filter(isDataVisible(_, userKey))
    write(data)
  }

  get("/dataset/:id") {
    contentType = "text/json"
    val reqId = paramOrHalt("id")
    val data = datasetStore.getItems(tconfig.instanceURI).
      find(_.id == reqId).getOrElse(halt(400))
    write(data)
  }

  get("/batch/dataset/:dataset") {
    contentType = "text/json"
    val requestedDatasetId = paramOrHalt("dataset")
    val exists = datasetStore.list(tconfig.instanceURI).contains(requestedDatasetId)
    if (!exists) halt(400)

    val batchStore = new BatchStore(baseConfig.triplestoreConfig)
    val r = batchStore.items(tconfig.instanceURI, Some(requestedDatasetId))
    write(r)
  }

  protected def overviewParameters: Seq[Attribute] =
    Seq(SampleId, Type, Organism, TestType, Repeat, Organ, Compound, DoseLevel,
      ExposureTime, CoreParameter.Platform, Treatment)

  get("/sample/batch/:batch") {
    contentType = "text/json"
    val requestedBatchId = paramOrHalt("batch")

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
    contentType = "text/json"
    val sf = SampleFilter(tconfig.instanceURI, None)
    val data = sampleStore.sampleQuery(SampleClassFilter(Map(Treatment -> paramOrHalt("treatment"))), sf)()
    write(data.map(sampleToMap))
  }

  /**
   * This request allows arbitrary GET parameter attribute filters, e.g. ?doseLevel=High
   */
  get("/sample") {
    contentType = "text/json"
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
    contentType = "text/json"
    val requestedParam = paramOrHalt("param")
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

  post("/matrix") {
    contentType = "text/json"
    val matParams: json.MatrixParams = read[json.MatrixParams](request.body)
    println(s"Load request: $matParams")
    val valueType = ValueType.valueOf(
      params.getOrElse("valueType", "Folds"))

    val defaultPageSize = 100

    val offset = params.getOrElse("offset", "0").toInt
    val pageSize = params.get("limit") match {
      case Some(l) => l.toInt
      case None => defaultPageSize
    }

    val (matrix, page) = matrixHandling.findOrLoadMatrix(matParams, valueType, offset, pageSize)
    val numPages = (matrix.info.numRows.toFloat / pageSize).ceil.toInt
    val ci = matrixHandling.columnInfoToJS(matrix.info)

    //writeJs avoids the problem of Map[String, Any] not having an encoder
    write(Map(
      "columns" -> writeJs(ci),
      "sorting" -> writeJs(Map(
        "column" -> writeJs(matrix.sortColumn.getOrElse(-1)),
        "ascending" -> writeJs(matrix.sortAscending))
      ),
      "rows" -> writeJs(matrixHandling.rowsToJS(page, matrix.info)),
      "last_page" -> writeJs(numPages),
    ))
  }

  post("/network") {
    contentType = "text/json"
    val netParams: json.NetworkParams = read[json.NetworkParams](request.body)
    println(s"Load request: $netParams")
    val valueType = ValueType.valueOf(
      params.getOrElse("valueType", "Folds"))
    val defaultLimit = 100
    val pageSize = params.get("limit") match {
      case Some(l) => l.toInt
      case None => defaultLimit
    }
    val network = networkHandling.loadNetwork(valueType, pageSize, netParams)
    write(Map(
      "interactions" -> networkHandling.interactionsToJson(network.interactions().asScala)
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
   * Example request: curl http://127.0.0.1:8888/json/association/GOBP/003017689013\?probes\=213646_x_at,213060_s_at
   */
  get("/association/:assoc/:sample") {
    contentType = "text/json"
    val probes = params.getOrElse("probes", halt(400))
    try {
      val requestedType = AType.valueOf(paramOrHalt("assoc"))
      val reprSample = paramOrHalt("sample")
      println(s"Get AType $requestedType for $probes and representative sample $reprSample")
      val limit = params.getOrElse("limit", "100").toInt
      val assoc = matrixHandling.association(requestedType, probes.split(","), reprSample, limit)
      write(associationToJSON(assoc))
    } catch {
      case iae: IllegalArgumentException =>
        System.err.println(s"Unknown association ${paramOrHalt("assoc")}")
        halt(400)
    }
  }

  /**
   * Obtain attributes for a batch
   */
  get("/attribute/batch/:batch") {
    contentType = "text/json"
    // we ignore this parameter for now because per-batch attributes
    // aren't implemented yet
    val batch = paramOrHalt("batch")

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
    contentType = "text/json"
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

    val authorizationCode = paramOrHalt("code")
    val verifier: String = session("verifier").asInstanceOf[String]

    val tokenResponse = authentication
      .exchangeOAuthCodeForAccessToken(authorizationCode, verifier)

    if (tokenResponse.wasSuccessful()) {
      val responseContent = tokenResponse.successResponse
      val accessToken = responseContent.token
      val refreshToken = responseContent.refreshToken

      response.addHeader("Set-Cookie", s"__Host-jwt=$accessToken; Secure; Path=/; HttpOnly; SameSite=Strict")
      response.addHeader("Set-Cookie", s"__Host-refreshToken=$refreshToken; Secure; Path=/; HttpOnly; SameSite=Strict")

      redirect("/admin/")
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
      case Right(error) => {
        println(error)
        halt(401)
      }
    }
  }

  def verifyRole(role: String): JWT = {
    val jwt = verifyJWT()
    val roles = jwt.getList("roles").asInstanceOf[util.List[String]]
    if (!roles.contains(role)) halt(401, s"You do not have role $role") else jwt
  }

  get("/check-cookie") {
    verifyJWT()
  }

  get("/roles") {
    val jwt = verifyJWT()
    val roles = jwt.getList("roles").asInstanceOf[util.List[String]].asScala
    writeJs(roles)
  }

  get("/logout") {
    for (name <- Array("__Host-jwt", "__Host-refreshToken")) {
      response.addHeader("Set-Cookie", s"$name=; Secure; Path=/; HttpOnly; SameSite=Strict; expires=Thu, Jan 01 1970 00:00:00 UTC;")
    }
    redirect(authentication.logoutUrl)
  }

  get("/batch") {
    verifyRole("admin")
    contentType = "text/json"

    val batchStore = new BatchStore(baseConfig.triplestoreConfig)
    val r = batchStore.getItems(None).map(batch =>  writeJs(Map(
      "id" -> writeJs(batch.id),
      "timestamp" -> writeJs(batch.timestamp),
      "comment" -> writeJs(batch.comment),
      "publicComment" -> writeJs(batch.publicComment),
      "dataset" -> writeJs(batch.dataset),
      "numSamples" -> writeJs(batch.numSamples),
      "enabledInstances" -> writeJs((batchStore.listAccess(batch.id).toSet))
    )))
    write(r)
  }

  /** Upload a batch
   * Example curl command:
   * curl -X POST -F metadata=@vitamin_a_metadata_full.tsv -F callsData=@vitamin_a_call.csv
   *   -F exprData=@vitamin_a_expr.csv http://127.0.0.1:4200/json/uploadBatch?batch=vatest
   * */
  post("/batch") {
    verifyRole("admin")

    val id = paramOrHalt("id")
    val comment = params.get("comment").getOrElse("")
    val publicComment = params.get("publicComment").getOrElse("")
    val dataset = paramOrHalt("dataset")
    val instances = params.get("enabledInstances").map(_.split(",").toList).getOrElse(List())

    val metadata = fileParams.get("metadata")
    val expr = fileParams.get("exprData")
    val calls = fileParams.get("callsData")
    val probes = fileParams.get("probesData")

    if (metadata.isEmpty) {
      throw new Exception("No metadata file")
    }
    if (expr.isEmpty) {
      throw new Exception("No data file")
    }

    uploadHandling.addBatch(new Batch(id, null, comment, publicComment, dataset, 0),
      metadata.get, expr.get, calls, probes, instances)
    Ok("Task started")
  }

  put("/batch") {
    verifyRole("admin")

    val id = paramOrHalt("id")
    val comment = params.get("comment").getOrElse("")
    val publicComment = params.get("publicComment").getOrElse("")
    val dataset = paramOrHalt("dataset")
    val metadata = fileParams.get("metadata")
    val recalculate = params.get("recalculate").map(_ == "true").getOrElse(false)
    val instances = params.get("enabledInstances").map(_.split(",").toList).getOrElse(List())

    uploadHandling.updateBatch(new Batch(id, null, comment, publicComment, dataset, 0),
      metadata, instances, recalculate)
  }

  delete("/batch/:batch") {
    verifyRole("admin")
    val batchId = paramOrHalt("batch")
    uploadHandling.deleteBatch(batchId)
    Ok("Task started")
  }

  get("/dataset/all") {
    verifyRole("admin")
    contentType = "text/json"
    val data = datasetStore.getItems(None)
    write(data)
  }

  post("/dataset") {
    verifyRole("admin")

    val datasetStore = new DatasetStore(baseConfig.triplestoreConfig)

    val id = paramOrHalt("id")
    val comment = paramOrHalt("comment")
    val description = paramOrHalt("description")
    val publicComment = paramOrHalt("publicComment")

    if (!TRDF.isValidIdentifier(id)) {
      throw BatchUploadException.badID(
        s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }

    if (datasetStore.getList().contains(id)) {
      throw BatchUploadException.badID(s"The dataset $id already exists, please choose a different name")
    } else {
      datasetStore.addWithTimestamp(id, TRDF.escape(comment))
      datasetStore.setDescription(id, TRDF.escape(description))
      datasetStore.setPublicComment(id, TRDF.escape(publicComment))
    }
    Ok("dataset created")
  }

  put("/dataset") {
    verifyRole("admin")

    val id = paramOrHalt("id")
    val comment = paramOrHalt("comment")
    val description = paramOrHalt("description")
    val publicComment = paramOrHalt("publicComment")

    val ds = new DatasetStore(baseConfig.triplestoreConfig)
    ds.setComment(id, TRDF.escape(comment))
    ds.setDescription(id, TRDF.escape(description))
    ds.setPublicComment(id, TRDF.escape(publicComment))

    Ok("dataset updated")
  }

  delete("/dataset/:id") {
    verifyRole("admin")
    val datasetId = paramOrHalt("id")
    val datasetStore = new DatasetStore(baseConfig.triplestoreConfig)
    datasetStore.delete(datasetId)

    Ok("dataset deleted")
  }

  get("/instance") {
    verifyRole("admin")
    contentType = "text/json"

    val instanceStore = new InstanceStore(baseConfig.triplestoreConfig)
    val comments = instanceStore.getComments()
    val timestamps = instanceStore.getTimestamps()

    // TODO add access policy and Tomcat role name
    write(instanceStore.getList().map(instanceId => writeJs(Map(
      "id" -> writeJs(instanceId),
      "comment" -> writeJs(comments.getOrElse(instanceId, "")),
      "timestamp" -> writeJs(timestamps.get(instanceId).getOrElse(null))))))
  }

  post("/instance") {
    verifyRole("admin")
    val id = paramOrHalt("id")
    val comment = paramOrHalt("comment")

    val instanceStore = new InstanceStore(baseConfig.triplestoreConfig)
    instanceStore.addWithTimestamp(id, TRDF.escape(comment))
    Ok("Instance added")
  }

  put("/instance") {
    verifyRole("admin")

    val id = paramOrHalt("id")
    val comment = paramOrHalt("comment")

    val instanceStore = new InstanceStore(baseConfig.triplestoreConfig)
    if (!instanceStore.getList().contains(id)) {
      throw new Exception(s"The instance $id does not exist")
    }
    instanceStore.setComment(id, TRDF.escape(comment))
    Ok("instance updated")
  }

  delete("/instance/:id") {
    verifyRole("admin")
    val instanceStore = new InstanceStore(baseConfig.triplestoreConfig)
    val id = paramOrHalt("id")

    if (!instanceStore.getList().contains(id)) {
      throw new Exception(s"The instance $id already exists, please choose a different name")
    }
    instanceStore.delete(id)
    Ok("Instance deleted")
  }

  get("/platform") {
    verifyRole("admin")
    contentType = "text/json"

    val probeStore = new ProbeStore(baseConfig.triplestoreConfig)
    val numProbes = probeStore.numProbes()
    val platformStore = new PlatformStore(baseConfig)
    val comments = platformStore.getComments()
    val pubComments = platformStore.getPublicComments()
    val dates = platformStore.getTimestamps()

    contentType = "text/json"
    write(platformStore.getList().map(id => writeJs(Map(
      "id" -> writeJs(id),
      "comment" -> writeJs(comments.getOrElse(id, "")),
      "date" -> writeJs(dates.getOrElse(id, null)),
      "probes" -> writeJs(numProbes.getOrElse(id, 0)),
      "publicComment" -> writeJs(pubComments.getOrElse(id, ""))
    ))))
  }

  post("/platform") {
    verifyRole("admin")

    val id = paramOrHalt("id")
    val platformType = paramOrHalt("type")
    val comment = paramOrHalt("comment")
    val publicComment = paramOrHalt("publicComment")

    val platformFormat = platformType match {
      case "Standard" => GeneralPlatform
      case "Affymetrix" => AffymetrixPlatform
      case "Biological" => BioPlatform
      case _ => throw new Exception("Invalid platform type.")
    }

    val platformFile = fileParams.get("platformFile")
    if (platformFile.isEmpty) {
      throw new Exception("No platform file")
    }

    uploadHandling.addPlatform(id, comment, publicComment, platformFormat, platformFile.get)
    Ok("Task started")
  }

  put("/platform") {
    verifyRole("admin")

    val id = paramOrHalt("id")
    val comment = paramOrHalt("comment")
    val publicComment = paramOrHalt("publicComment")

    val platformStore = new PlatformStore(baseConfig)
    platformStore.setComment(id, comment)
    platformStore.setPublicComment(id, publicComment)
    Ok("platform updated")
  }

  delete("/platform/:id") {
    verifyRole("admin")

    val id = paramOrHalt("id")

    uploadHandling.deletePlatform(id)
    Ok("Task started")
  }

  /**
   * Obtain the latest log messages (seizing them and removing them from the server).
   * Task: associate log messages with the user that started the current task, so they cannot be maliciously removed
   */
  get("/taskProgress") {
    verifyRole("admin")
    contentType = "text/json"
    write(uploadHandling.getProgress())
  }
}
