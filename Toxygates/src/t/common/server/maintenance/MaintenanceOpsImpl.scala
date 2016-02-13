package t.common.server.maintenance

import gwtupload.server.UploadServlet
import t.common.shared.maintenance.MaintenanceException
import t.util.TempFiles
import t.TaskRunner
import t.viewer.server.rpc.TServiceServlet
import t.common.shared.maintenance.OperationResults
import org.apache.commons.fileupload.FileItem
import t.common.shared.maintenance.Progress
import scala.collection.JavaConversions._
import javax.servlet.http.HttpServletRequest
import t.global.KCDBRegistry

/**
 * Servlet routines for uploading files and running tasks.
 */
trait MaintenanceOpsImpl extends t.common.client.rpc.MaintenanceOperations {
  this: TServiceServlet =>

  //Methods for accessing the http request's thread local state.
  //Must be abstract here or an access error will be thrown at runtime.
  //(Implement in the concrete subclass)
  protected def getAttribute[T](name: String): T
  protected def setAttribute(name: String, x: AnyRef): Unit
  protected def request: HttpServletRequest

  protected def setLastTask(task: String) = setAttribute("lastTask", task)
  protected def lastTask: String = getAttribute("lastTask")
  protected def setLastResults(results: OperationResults) = setAttribute("lastResults", results)
  protected def lastResults: OperationResults = getAttribute("lastResults")

  protected def afterTaskCleanup() {
    val tc: TempFiles = getAttribute("tempFiles")
    if (tc != null) {
    	tc.dropAll()
    }
    UploadServlet.removeSessionFileItems(request)
    TaskRunner.shutdown()
    KCDBRegistry.closeWriters()
  }

  protected def beforeTaskCleanup() {
    UploadServlet.removeSessionFileItems(request)
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

  protected def grabRunner() {
    if (TaskRunner.queueSize > 0 || TaskRunner.waitingForTask) {
	  throw new Exception("Another task is already in progress.")
	}
  }

  protected def maintenance[T](task: => T): T = try {
    task
  } catch {
    case e: Exception =>
      e.printStackTrace()
      throw new MaintenanceException(e)
  }

  protected def cleanMaintenance[T](task: => T): T = try {
    task
  } catch {
    case e: Exception =>
      e.printStackTrace()
      afterTaskCleanup()
      throw new MaintenanceException(e)
  }

  /**
   * Get an uploaded file as a temporary file.
   * Should be deleted after use.
   */
  protected def getAsTempFile(tempFiles: TempFiles, tag: String, prefix: String,
      suffix: String): Option[java.io.File] = {
    getFile(tag) match {
      case None => None
      case Some(fi) =>
        val f = tempFiles.makeNew(prefix, suffix)
        fi.write(f)
        Some(f)
    }
  }

   /**
   * Retrive the last uploaded file with a particular tag.
   * @param tag the tag to look for.
   * @return
   */
  protected def getFile(tag: String): Option[FileItem] = {
    val items = UploadServlet.getSessionFileItems(request);
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

  protected def showUploadedFiles(): Unit = {
    val items = UploadServlet.getSessionFileItems(request)
    if (items != null) {
      for (fi <- items) {
        System.out.println("File " + fi.getName() + " size "
          + fi.getSize() + " field: " + fi.getFieldName())
      }
    }
  }

}
