package t.admin.server

import scala.collection.JavaConversions._

import org.apache.commons.fileupload.FileItem

import com.google.gwt.user.server.rpc.RemoteServiceServlet

import gwtupload.server.UploadServlet
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otgviewer.server.Configuration
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

class MaintenanceServiceImpl extends RemoteServiceServlet with MaintenanceService {
  var baseConfig: BaseConfig = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    
    val conf = Configuration.fromServletConfig(config)
    baseConfig = conf.baseConfig
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
    if (TaskRunner.currentTask != None) {
	  throw new Exception("Another task is already in progress.")
	}
  }
  
  def addBatchAsync(title: String, comment: String): Unit = {
	showUploadedFiles()
	grabRunner()
	
	val bm = new BatchManager(baseConfig) //TODO configuration parsing

    try {
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
      val niFile = getAsTempFile(tempFiles, niPrefix, niPrefix, "csv").get
      val foldFile = getAsTempFile(tempFiles, foldPrefix, foldPrefix, "csv").get
      val callsFile = getAsTempFile(tempFiles, callPrefix, callPrefix, "csv")
      val foldCallsFile = getAsTempFile(tempFiles, foldCallPrefix, foldCallPrefix, "csv")
      val foldPValueFile = getAsTempFile(tempFiles, foldPPrefix, foldPPrefix, "csv")

      TaskRunner ++= bm.addBatch(title, comment, 
        metaFile.getAbsolutePath(),
        niFile.getAbsolutePath(),
        callsFile.map(_.getAbsolutePath()),
        foldFile.getAbsolutePath(),
        foldCallsFile.map(_.getAbsolutePath()),
        foldPValueFile.map(_.getAbsolutePath()))
        
    } catch {
	  case e: Exception =>
	    afterTaskCleanup()
	    throw e	  
	}
  }

  def addPlatformAsync(id: String, comment: String, affymetrixFormat: Boolean): Unit = {    
    showUploadedFiles()
	grabRunner()
	val pm = new PlatformManager(baseConfig) //TODO configuration parsing
    try {      
      val tempFiles = new TempFiles()
      setAttribute("tempFiles", tempFiles)
      
      TaskRunner.start() 
      setLastTask("Add platform")
      if (getFile(platformPrefix) == None) {
        throw new MaintenanceException("The platform file has not been uploaded yet.")
      }
      
      val metaFile = getAsTempFile(tempFiles, platformPrefix, platformPrefix, "dat").get
      TaskRunner ++= pm.addPlatform(id, comment, metaFile.getAbsolutePath(), affymetrixFormat)
    } catch {
      case e: Exception =>
        afterTaskCleanup()
        throw e
    }
  }

  def deleteBatchAsync(id: String): Unit = {
    grabRunner()
    val bm = new BatchManager(baseConfig) //TODO configuration parsing
    try {
      TaskRunner.start()
      setLastTask("Delete batch")
      TaskRunner ++= bm.deleteBatch(id)
    } catch {
      case e: Exception =>
        afterTaskCleanup()
        throw e
    }
  }

  def deletePlatformAsync(id: String): Unit = {
    grabRunner()
    val pm = new PlatformManager(baseConfig)
    try {
      TaskRunner.start()
      setLastTask("Delete platform")
      TaskRunner ++= pm.deletePlatform(id)
    } catch {
      case e: Exception =>
        afterTaskCleanup()
        throw e
    }
  }
  
  def getOperationResults(): OperationResults = {
    lastResults
  }

  def cancelTask(): Unit = {
    //TODO
    afterTaskCleanup()	
  }

  def getProgress(): Progress = {    
    val messages = TaskRunner.logMessages.toArray
    for (m <- messages) {
      println(m)
    }
    val p = TaskRunner.currentTask match {
      case None => new Progress("No task in progress", 0, true)
      case Some(t) => new Progress(t.name, t.percentComplete, false)
    }
    p.setMessages(messages)
    
    if (TaskRunner.currentTask == None) {
      setLastResults(new OperationResults(lastTask, 
          TaskRunner.errorCause == None, 
          TaskRunner.resultMessages.toArray))      
      afterTaskCleanup()
    }
    p    
  }
  
  def getBatches: Array[Batch] = {
    val bs = new Batches(baseConfig.triplestore)
    val ns = bs.numSamples
    val comments = bs.comments
    val dates = bs.timestamps
    bs.list.map(b => {
      val samples = ns.getOrElse(b, 0)
      new Batch(b, samples, comments.getOrElse(b, ""),
          dates.getOrElse(b, null), Array[String]())
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
    Array()
  }
  
  def updateBatch(b: Batch): Unit = {
    
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