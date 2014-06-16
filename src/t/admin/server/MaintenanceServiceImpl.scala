package t.admin.server

import scala.collection.JavaConversions._
import org.apache.commons.fileupload.FileItem
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import gwtupload.server.UploadServlet
import javax.annotation.Nullable
import t.BaseConfig
import t.admin.client.MaintenanceService
import t.admin.shared.AddBatchResult
import t.admin.shared.AddPlatformResult
import t.admin.shared.Progress
import t.TriplestoreConfig
import t.DataConfig
import t.TaskRunner
import t.BatchManager
import t.admin.shared.MaintenanceConstants._

class MaintenanceServiceImpl extends RemoteServiceServlet with MaintenanceService {
  def configuration(): BaseConfig = {
    //TODO
    BaseConfig(TriplestoreConfig("http://localhost:3030/data/query", "http://localhost:3030/data/update", 
        null, null, null),
        DataConfig("/shiba/toxygates/data_dev"))
  }

  
  def tryAddBatch(title: String): AddBatchResult = {
	showUploadedFiles()
	if (TaskRunner.busy) {
	  throw new Exception("Another task is already in progress.")
	}
	val bm = new BatchManager(configuration())
	TaskRunner.start()
	
	var tempFiles: List[java.io.File] = List()
	val metaFile = getAsTempFile(metaPrefix, metaPrefix, "tsv").
		getOrElse(throw new Exception("The metadata file has not been uploaded yet."))
	tempFiles ::= metaFile
    //TODO store tempFiles in HTTP session and do a cleanup later, as well as
	//print any remaining log messages

    TaskRunner ++= bm.addBatch(title, metaFile.getAbsolutePath(), null, null, null)
    null
  }

  def tryAddPlatform(): AddPlatformResult = {
    null
  }

  def tryDeleteBatch(id: String): Boolean = {
    false
  }

  def tryDeletePlatform(id: String): Boolean = {
    false;
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
      case Some(t) => new Progress(t.name, t.percentComplete, !TaskRunner.busy)
    }
    p.setMessages(messages)
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
  def getAsTempFile(tag: String, prefix: String, suffix: String): Option[java.io.File] = {
    getFile(tag) match {
      case None => None
      case Some(fi) =>
        val f = java.io.File.createTempFile(prefix, suffix)
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