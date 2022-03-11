package panomicon

import org.scalatra.servlet.FileItem
import t.Context
import t.global.KCDBRegistry
import t.manager.BatchManager.Batch
import t.manager.{BatchManager, PlatformManager, Task, TaskRunner}
import t.platform.PlatformFormat
import t.shared.common.maintenance.{BatchUploadException, MaintenanceException}
import t.sparql.{BatchStore, PlatformStore, TRDF}
import t.util.TempFiles
import ujson.Value
import upickle.default.writeJs

class UploadHandling(context: Context) {

  def getProgress(): Value = {
    TaskRunner.synchronized {
      val messages = TaskRunner.seizeLogMessages.toArray
      val p = if (TaskRunner.available) {
        ("No task in progress", 0, true, TaskRunner.errorCause)
      } else {
        TaskRunner.currentAtomicTask match {
          case Some(t) => (t.name, t.percentComplete, false, TaskRunner.errorCause)
          case None => ("Error", 0, false, TaskRunner.errorCause)
        }
      }
      writeJs(Map(
        "task" -> writeJs(p._1),
        "completion" -> writeJs(p._2),
        "finished" -> writeJs(p._3),
        "errorCause" -> writeJs(p._4.map(_.getMessage).getOrElse("")),
        "messages" -> writeJs(messages)
      ))
    }
  }

  private def itemToFile(tempFiles: TempFiles, key: String, item: FileItem) = {
    val f = tempFiles.makeNew(key, "tmp")
    item.write(f)
    f
  }
  private val batchManager = new BatchManager(context)

  def addBatch(batch: t.sparql.Batch, metadata: FileItem, exprData: FileItem, callsData: Option[FileItem],
               probesData: Option[FileItem], visibleInstances: List[String], mayAppendBatch: Boolean = true) {
    val tempFiles = new TempFiles()

    val metaFile = itemToFile(tempFiles, "metadata", metadata)
    val dataFile = itemToFile(tempFiles, "expr", exprData)
    val callsFile = callsData.map(c => itemToFile(tempFiles, "calls", c))
    val probesFile = probesData.map(p => itemToFile(tempFiles, "probes", p))

    ensureNotMaintenance()
    grabRunner()

    val existingBatches = new BatchStore(context.config.triplestoreConfig).getList()
    if (existingBatches.contains(batch.id) && !mayAppendBatch) {
      throw BatchUploadException.badID(
        s"The batch $batch already exists and appending is not allowed. " +
          "Please choose a different name.")
    }

    runTasks(batchManager.add(batch.toBatchManager(visibleInstances),
      metaFile.getAbsolutePath, dataFile.getAbsolutePath, callsFile.map(_.getAbsolutePath),
      append = false, generateAttributes = true, conversion = None), Some(tempFiles))
  }

  def updateBatch(batch: t.sparql.Batch, metadata: Option[FileItem], visibleInstances: List[String],
                  recalculate: Boolean): Unit = {
    val tempFiles = new TempFiles()
    val metaFile = metadata.map(c => itemToFile(tempFiles, "metadata", c))

    ensureNotMaintenance()
    grabRunner()

    val b = batch.toBatchManager(visibleInstances)
    val update = batchManager.updateBatch(b)
    val tasks = metaFile match {
      case Some(mf) => update.andThen(batchManager.updateMetadata(b, mf.getAbsolutePath,
        true, recalculate))
      case _ => update
    }
    runTasks(tasks, Some(tempFiles))
  }

  def deleteBatch(batch: String) {
    val batchManager = new BatchManager(context)
    runTasks(batchManager.delete(batch, false), None)
  }

  def addPlatform(id: String, comment: String, publicComment: String,
                  format: PlatformFormat, platform: FileItem): Unit = {
    val tempFiles = new TempFiles()

    val platformFile = itemToFile(tempFiles, "metadata", platform)

    ensureNotMaintenance()
    grabRunner()
    val pm = new PlatformManager(context)

    if (!TRDF.isValidIdentifier(id)) {
      throw new Exception(s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }

    runTasks(pm.add(id, TRDF.escape(comment),
      platformFile.getAbsolutePath(), format) andThen
      Task.simple("Set platform parameters"){
        val pfs = new PlatformStore(context.config)
        pfs.setComment(id, comment)
        pfs.setPublicComment(id, comment)
      }, Some(tempFiles))
  }

  def deletePlatform(id: String): Unit = {
    ensureNotMaintenance()
    grabRunner()
    val pm = new PlatformManager(context)
    runTasks(pm.delete(id), None)
  }

  protected def grabRunner() {
    if (!TaskRunner.available) {
      throw new MaintenanceException("Another task is already in progress.")
    }
  }

  protected def isMaintenanceMode: Boolean =
    context.config.data.isMaintenanceMode

  protected def ensureNotMaintenance(): Unit = {
    if (isMaintenanceMode) {
      throw new MaintenanceException("The system is currently in maintenance mode.\n" +
        "Please try again later.")
    }
  }

  protected def runTasks(task: Task[_], tempFiles: Option[TempFiles]) {
    grabRunner()
    TaskRunner.runThenFinally(task) {
      TaskRunner.log("Writing databases, this may take a while...")
      KCDBRegistry.closeWriters()
      TaskRunner.log("Databases written")
      TaskRunner.synchronized {
        try {
          for { tf <- tempFiles } {
            tf.dropAll()
          }
          val success = TaskRunner.errorCause == None
          //Task: associate success/failure with the user's session or ID
        } catch {
          case e: Exception =>
            println(e)
            e.printStackTrace()
        }
      }
    }
  }
}
