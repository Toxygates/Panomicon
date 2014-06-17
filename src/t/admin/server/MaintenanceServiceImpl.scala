package t.admin.server

import scala.collection.JavaConversions._
import org.apache.commons.fileupload.FileItem
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import gwtupload.server.UploadServlet
import javax.annotation.Nullable
import t.BaseConfig
import t.admin.client.MaintenanceService
import t.admin.shared.Progress
import t.TriplestoreConfig
import t.DataConfig
import t.TaskRunner
import t.BatchManager
import t.admin.shared.MaintenanceConstants._
import t.admin.shared.OperationResults
import t.util.TempFiles

class MaintenanceServiceImpl extends RemoteServiceServlet with MaintenanceService {
  def configuration(): BaseConfig = {
    //TODO
    BaseConfig(TriplestoreConfig("http://localhost:3030/data/query", "http://localhost:3030/data/update", 
        null, null, null),
        DataConfig("/shiba/toxygates/data_dev"))
  }
  
  private def getAttribute[T](name: String) = 
    getThreadLocalRequest().getSession().getAttribute(name).asInstanceOf[T]
  
  private def setAttribute(name: String, x: AnyRef) = 
  	getThreadLocalRequest().getSession().setAttribute(name, x)
  
  private def setLastTask(task: String) = setAttribute("lastTask", task)  
  private def lastTask: String = getAttribute("lastTask")
  private def setLastResults(results: OperationResults) = setAttribute("lastResults", results)    
  private def lastResults: OperationResults = getAttribute("lastResults")    
  
  private def afterTaskCleanup(): Unit = {
    val tc: TempFiles = getAttribute("tempFiles")
    tc.dropAll()
  }
  
  def tryAddBatch(title: String): Unit = {
	showUploadedFiles()
	if (TaskRunner.currentTask != None) {
	  throw new Exception("Another task is already in progress.")
	}
	val bm = new BatchManager(configuration()) //TODO configuration parsing
	TaskRunner.start()
	setLastTask("Add batch")
	
	val tempFiles = new TempFiles()
	setAttribute("tempFiles", tempFiles)	
	
	val metaFile = getAsTempFile(tempFiles, metaPrefix, metaPrefix, "tsv").
		getOrElse(throw new Exception("The metadata file has not been uploaded yet."))
	
	val niFile = getAsTempFile(tempFiles, niPrefix, niPrefix, "csv").
		getOrElse(throw new Exception("The normalized intensity file has not been uploaded yet."))
	val mas5File = getAsTempFile(tempFiles, mas5Prefix, mas5Prefix, "csv").
		getOrElse(throw new Exception("The MAS5 normalized file has not been uploaded yet."))	
	val callsFile = getAsTempFile(tempFiles, callPrefix, callPrefix, "csv"). 
		getOrElse(throw new Exception("The calls file has not been uploaded yet.")) 
	
    TaskRunner ++= bm.addBatch(title, metaFile.getAbsolutePath(), 
        niFile.getAbsolutePath(), 
        mas5File.getAbsolutePath(), 
        callsFile.getAbsolutePath())   
  }

  def tryAddPlatform(): Unit = {    
  }

  def tryDeleteBatch(id: String): Boolean = {
    false
  }

  def tryDeletePlatform(id: String): Boolean = {
    false;
  }
  
  def getOperationResults(): OperationResults = {
    lastResults
  }

  def cancelTask(): Unit = {
    //TODO
	TaskRunner.shutdown()
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

  /**
   * Retrive the last uploaded file with a particular tag.
   * @param tag the tag to look for.
   * @return
   */
  def getFile(tag: String): Option[FileItem] = {
    var last: FileItem = null
    val items = UploadServlet.getSessionFileItems(getThreadLocalRequest());
    for (fi <- items) {
      if (fi.getFieldName().startsWith(tag)) {
        last = fi
      }
    }    
    if (last == null) {
      None
    } else {
      Some(last)
    }    
  }
  
  /**
   * Get an uploaded file as a temporary file.
   * Should be deleted after use.
   */
  def getAsTempFile(tempFiles: TempFiles, tag: String, prefix: String, 
      suffix: String): Option[java.io.File] = {
    getFile(tag) match {
      case None => None
      case Some(fi) =>        
        val f = tempFiles.makeNew(prefix, suffix)
        fi.write(f)
        Some(f)
    }
  }
  
  def showUploadedFiles(): Unit = {
    val items = UploadServlet.getSessionFileItems(getThreadLocalRequest())
    if (items != null) {
      for (fi <- items) {
        System.out.println("File " + fi.getName() + " size "
          + fi.getSize() + " field: " + fi.getFieldName())
      }
    }    
  }
}