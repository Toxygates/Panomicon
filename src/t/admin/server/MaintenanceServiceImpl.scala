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
import t.common.server.rpc.TServiceServlet
import t.common.shared.Dataset
import t.common.server.SharedDatasets

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
  
  def addBatchAsync(title: String, comment: String): Unit = {
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
      if (getFile(niPrefix) == None) {
        throw new MaintenanceException("The normalized intensity file has not been uploaded yet.")
      }
      if (getFile(foldPrefix) == None) {
        throw new MaintenanceException("The fold expression file has not been uploaded yet.")
      }

      val metaFile = getAsTempFile(tempFiles, metaPrefix, metaPrefix, "tsv").get
      val niFile = getAsTempFile(tempFiles, niPrefix, niPrefix, "csv")
      val foldFile = getAsTempFile(tempFiles, foldPrefix, foldPrefix, "csv").get
      val callsFile = getAsTempFile(tempFiles, callPrefix, callPrefix, "csv")
      val foldCallsFile = getAsTempFile(tempFiles, foldCallPrefix, foldCallPrefix, "csv")
      val foldPValueFile = getAsTempFile(tempFiles, foldPPrefix, foldPPrefix, "csv")

      val md = factory.tsvMetadata(metaFile.getAbsolutePath())
      TaskRunner ++= bm.addBatch(title, comment, md,
        niFile.map(_.getAbsolutePath()),
        callsFile.map(_.getAbsolutePath()),
        foldFile.getAbsolutePath(),
        foldCallsFile.map(_.getAbsolutePath()),
        foldPValueFile.map(_.getAbsolutePath()),
        false, baseConfig.seriesBuilder)        
    }
  }

  def addPlatformAsync(id: String, comment: String, affymetrixFormat: Boolean): Unit = {    
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
      
      val metaFile = getAsTempFile(tempFiles, platformPrefix, platformPrefix, "dat").get
      TaskRunner ++= pm.addPlatform(id, comment, metaFile.getAbsolutePath(), affymetrixFormat)
    } 
  }

  def addInstance(i: Instance): Unit = {
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

  def addDataset(d: Dataset): Unit = {
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
  
  def deleteInstance(id: String): Unit = {
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
  
  def deleteDataset(id: String): Unit = {
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
    val dates = ps.timestamps
    ps.list.map(p => {
      new Platform(p, np.getOrElse(p, 0), comments.getOrElse(p, ""),
          dates.getOrElse(p, null))
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
  
  def updateBatch(b: Batch): Unit = {
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