/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package t.admin.server

import scala.collection.JavaConversions._
import org.apache.commons.fileupload.FileItem
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import gwtupload.server.UploadServlet
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import t.viewer.server.Configuration
import t.BaseConfig
import t.BatchManager
import t.PlatformManager
import t.TaskRunner
import t.TriplestoreConfig
import t.admin.client.MaintenanceService
import t.admin.shared.Batch
import t.admin.shared.Instance
import t.admin.shared.MaintenanceConstants._
import t.admin.shared.MaintenanceException
import t.admin.shared.OperationResults
import t.admin.shared.Platform
import t.admin.shared.Progress
import t.sparql.Batches
import t.sparql.Platforms
import t.sparql.Probes
import t.util.TempFiles
import t.DataConfig
import otg.OTGBConfig
import t.sparql.Instances
import t.InstanceManager
import t.sparql.TRDF
import scala.sys.process._
import t.common.shared.Dataset
import t.sparql.Datasets
import t.viewer.server.rpc.TServiceServlet
import t.common.shared.Dataset
import t.common.server.SharedDatasets
import t.common.shared.ManagedItem

abstract class MaintenanceServiceImpl extends TServiceServlet with MaintenanceService {

  private var homeDir: String = _

  override def localInit(config: Configuration) {
    super.localInit(config)
    homeDir = config.webappHomeDir
  }

  private def getAttribute[T](name: String) =
    getThreadLocalRequest().getSession().getAttribute(name).asInstanceOf[T]

  private def setAttribute(name: String, x: AnyRef) =

  getThreadLocalRequest().getSession().setAttribute(name, x)
  private def setLastTask(task: String) = setAttribute("lastTask", task)
  private def lastTask: String = getAttribute("lastTask")
  private def setLastResults(results: OperationResults) = setAttribute("lastResults", results)
  private def lastResults: OperationResults = getAttribute("lastResults")

  private def afterTaskCleanup() {
    val tc: TempFiles = getAttribute("tempFiles")
    if (tc != null) {
    	tc.dropAll()
    }
    UploadServlet.removeSessionFileItems(getThreadLocalRequest())
    TaskRunner.shutdown()
  }

  private def beforeTaskCleanup() {
    UploadServlet.removeSessionFileItems(getThreadLocalRequest())
  }

  private def grabRunner() {
    if (TaskRunner.queueSize > 0 || TaskRunner.waitingForTask) {
	  throw new Exception("Another task is already in progress.")
	}
  }

  private def maintenance[T](task: => T): T = try {
    task
  } catch {
    case e: Exception =>
      e.printStackTrace()
      throw new MaintenanceException(e)
  }

  private def cleanMaintenance[T](task: => T): T = try {
    task
  } catch {
    case e: Exception =>
      e.printStackTrace()
      afterTaskCleanup()
      throw new MaintenanceException(e)
  }

  def addBatchAsync(b: Batch): Unit = {
	  showUploadedFiles()
	  grabRunner()

	  val bm = new BatchManager(context) //TODO configuration parsing

    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Add batch")

      val tempFiles = new TempFiles()
      setAttribute("tempFiles", tempFiles)

      if (getFile(metaPrefix) == None) {
        throw new MaintenanceException("The metadata file has not been uploaded yet.")
      }
      if (getFile(dataPrefix) == None) {
        throw new MaintenanceException("The normalized intensity file has not been uploaded yet.")
      }

      val metaFile = getAsTempFile(tempFiles, metaPrefix, metaPrefix, "tsv").get
      val dataFile = getAsTempFile(tempFiles, dataPrefix, dataPrefix, "csv")
          val callsFile = getAsTempFile(tempFiles, callPrefix, callPrefix, "csv")

      val md = factory.tsvMetadata(metaFile.getAbsolutePath())
      TaskRunner ++= bm.addBatch(b.getTitle, b.getComment, md,
        dataFile.get.getAbsolutePath(),
        callsFile.map(_.getAbsolutePath()),
        false, baseConfig.seriesBuilder)
    }
  }

  def addPlatformAsync(p: Platform, affymetrixFormat: Boolean): Unit = {
    showUploadedFiles()
	grabRunner()
	val pm = new PlatformManager(context) //TODO configuration parsing

    cleanMaintenance {
      val tempFiles = new TempFiles()
      setAttribute("tempFiles", tempFiles)

      TaskRunner.start()
      setLastTask("Add platform")
      if (getFile(platformPrefix) == None) {
        throw new MaintenanceException("The platform file has not been uploaded yet.")
      }

      val id = p.getTitle
      val comment = p.getComment

      if (!TRDF.isValidIdentifier(id)) {
        throw new MaintenanceException(
          s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
      }

      val metaFile = getAsTempFile(tempFiles, platformPrefix, platformPrefix, "dat").get
      TaskRunner ++= pm.addPlatform(id, TRDF.escape(comment), metaFile.getAbsolutePath(), affymetrixFormat)
    }
  }

  def add(i: ManagedItem): Unit = {
    i match {
      case d: Dataset => addDataset(d)
      case i: Instance => addInstance(i)
      case _ => throw new MaintenanceException(s"Illegal API usage, cannot add $i")
    }
  }

  private def addInstance(i: Instance): Unit = {
    val im = new Instances(baseConfig.triplestore)

    val id = i.getTitle()
    if (!TRDF.isValidIdentifier(id)) {
      throw new MaintenanceException(
          s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }
    val param = i.getPolicyParameter()
    if (!TRDF.isValidIdentifier(param)) {
      throw new MaintenanceException(
          s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }
    if (im.list.contains(id)) {
      throw new MaintenanceException(s"The instance $id already exists, please choose a different name")
    }

    maintenance {
      val ap = i.getAccessPolicy()
      val policy = ap.toString().toLowerCase();

      val cmd = s"sh $homeDir/new_instance.${policy}.sh $id $id $param"
      println(s"Run command: $cmd")
      val p = Process(cmd).!
      if (p != 0) {
        throw new MaintenanceException(s"Creating webapp instance failed: return code $p")
      }
      im.addWithTimestamp(id, TRDF.escape(i.getComment))
    }
  }

  private def addDataset(d: Dataset): Unit = {
    val dm = new Datasets(baseConfig.triplestore)

    val id = d.getTitle()
    if (!TRDF.isValidIdentifier(id)) {
      throw new MaintenanceException(
        s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }

    if (dm.list.contains(id)) {
      throw new MaintenanceException(s"The dataset $id already exists, please choose a different name")
    }

    maintenance {
      dm.addWithTimestamp(id, TRDF.escape(d.getComment))
      dm.setDescription(id, TRDF.escape(d.getDescription))
    }
  }

  def deleteBatchAsync(id: String): Unit = {
    grabRunner()
    val bm = new BatchManager(context) //TODO configuration parsing
    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Delete batch")
      TaskRunner ++= bm.deleteBatch(id, baseConfig.seriesBuilder)
    }
  }

  def deletePlatformAsync(id: String): Unit = {
    grabRunner()
    val pm = new PlatformManager(context)
    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Delete platform")
      TaskRunner ++= pm.deletePlatform(id)
    }
  }

  def delete(i: ManagedItem): Unit = {
    i match {
      case i: Instance => deleteInstance(i.getTitle)
      case d: Dataset => deleteDataset(d.getTitle)
      case _ => throw new MaintenanceException("Illegal API usage")
    }
  }

  private def deleteInstance(id: String): Unit = {
    val im = new Instances(baseConfig.triplestore)
    maintenance {
      val cmd = s"sh $homeDir/delete_instance.sh $id $id"
      println(s"Run command: $cmd")
      val p = Process(cmd).!
      if (p != 0) {
        throw new MaintenanceException(s"Deleting webapp instance failed: return code $p")
      }

      im.delete(id)
    }
  }

  private def deleteDataset(id: String): Unit = {
    val dm = new Datasets(baseConfig.triplestore)
    maintenance {
      dm.delete(id)
    }
  }

  def getOperationResults(): OperationResults = {
    lastResults
  }

  def cancelTask(): Unit = {
    TaskRunner.log("* * * Cancel requested * * *")
    TaskRunner.shutdown()
    //It may still take time for the current task to respond to the cancel request.
    //Progress should be checked repeatedly.
  }

  def getProgress(): Progress = {
    TaskRunner.synchronized {
      val messages = TaskRunner.logMessages.toArray
      for (m <- messages) {
        println(m)
      }
      val p = if (TaskRunner.queueSize == 0 && !TaskRunner.waitingForTask) {
        setLastResults(new OperationResults(lastTask,
        		   TaskRunner.errorCause == None,
        		   TaskRunner.resultMessages.toArray))
          afterTaskCleanup()
          new Progress("No task in progress", 0, true)
      } else {
        TaskRunner.currentTask match {
          case Some(t) => new Progress(t.name, t.percentComplete, false)
          case None => new Progress("??", 0, false)
        }
      }
      p.setMessages(messages)
      p
    }
  }

  import java.util.HashSet

  def getBatches: Array[Batch] = {
    val bs = new Batches(baseConfig.triplestore)
    val ns = bs.numSamples
    val comments = bs.comments
    val dates = bs.timestamps
    val datasets = bs.datasets
    bs.list.map(b => {
      val samples = ns.getOrElse(b, 0)
      new Batch(b, samples, comments.getOrElse(b, ""),
          dates.getOrElse(b, null),
          new HashSet(setAsJavaSet(bs.listAccess(b).toSet)),
          datasets.getOrElse(b, ""))
    }).toArray
  }

  def getPlatforms: Array[Platform] = {
    val prs = new Probes(baseConfig.triplestore)
    val np = prs.numProbes()
    val ps = new Platforms(baseConfig.triplestore)
    val comments = ps.comments
    val pubComments = ps.publicComments
    val dates = ps.timestamps
    ps.list.map(p => {
      new Platform(p, np.getOrElse(p, 0), comments.getOrElse(p, ""),
          dates.getOrElse(p, null), pubComments.getOrElse(p, ""))
    }).toArray
  }

  def getInstances: Array[Instance] = {
    val is = new Instances(baseConfig.triplestore)
    val com = is.comments
    val ts = is.timestamps
    is.list.map(i => new Instance(i, com.getOrElse(i, ""),
        ts.getOrElse(i, null))).toArray
  }

  def getDatasets: Array[Dataset] = {
    val ds = new Datasets(baseConfig.triplestore) with SharedDatasets
    ds.sharedList.toArray
  }

  def update(i: ManagedItem): Unit = {
    i match {
      case b: Batch => updateBatch(b)
      case i: Instance => updateInstance(i)
      case p: Platform => updatePlatform(p)
      case d: Dataset => updateDataset(d)
    }
  }

  private def updateBatch(b: Batch): Unit = {
    val bs = new Batches(baseConfig.triplestore)
    val existingAccess = bs.listAccess(b.getTitle())
    val newAccess = b.getEnabledInstances()
    for (i <- newAccess; if !existingAccess.contains(i)) {
      bs.enableAccess(b.getTitle(), i)
    }
    for (i <- existingAccess; if !newAccess.contains(i)) {
      bs.disableAccess(b.getTitle(), i)
    }

    val oldDs = bs.datasets.getOrElse(b.getTitle, null)
    val newDs = b.getDataset
    if (newDs != oldDs) {
      val ds = new Datasets(baseConfig.triplestore)
      if (oldDs != null) {
        ds.removeMember(b.getTitle, oldDs)
      }
      ds.addMember(b.getTitle, newDs)
    }
    bs.setComment(b.getTitle, TRDF.escape(b.getComment))
  }

  private def updatePlatform(p: Platform): Unit = {
    val pfs = new Platforms(baseConfig.triplestore)
    pfs.setComment(p.getTitle, p.getComment)
    pfs.setPublicComment(p.getTitle, p.getPublicComment)
  }

  private def updateDataset(d: Dataset): Unit = {
    val ds = new Datasets(baseConfig.triplestore)
    ds.setComment(d.getTitle, TRDF.escape(d.getComment))
    ds.setPublicComment(d.getTitle, TRDF.escape(d.getPublicComment))
  }

  private def updateInstance(i: Instance): Unit = {
    val is = new Instances(baseConfig.triplestore)
    is.setComment(i.getTitle, TRDF.escape(i.getComment))
  }

  /**
   * Retrive the last uploaded file with a particular tag.
   * @param tag the tag to look for.
   * @return
   */
  private def getFile(tag: String): Option[FileItem] = {
    val items = UploadServlet.getSessionFileItems(getThreadLocalRequest());
    if (items == null) {
      throw new MaintenanceException("No files have been uploaded yet.")
    }

    for (fi <- items) {
      if (fi.getFieldName().startsWith(tag)) {
        return Some(fi)
      }
    }
    return None
  }

  /**
   * Get an uploaded file as a temporary file.
   * Should be deleted after use.
   */
  private def getAsTempFile(tempFiles: TempFiles, tag: String, prefix: String,
      suffix: String): Option[java.io.File] = {
    getFile(tag) match {
      case None => None
      case Some(fi) =>
        val f = tempFiles.makeNew(prefix, suffix)
        fi.write(f)
        Some(f)
    }
  }

  private def showUploadedFiles(): Unit = {
    val items = UploadServlet.getSessionFileItems(getThreadLocalRequest())
    if (items != null) {
      for (fi <- items) {
        System.out.println("File " + fi.getName() + " size "
          + fi.getSize() + " field: " + fi.getFieldName())
      }
    }
  }
}
