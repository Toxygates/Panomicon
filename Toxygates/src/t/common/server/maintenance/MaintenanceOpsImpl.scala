/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

  protected def isMaintenanceMode: Boolean =
    context.config.data.isMaintenanceMode

  protected def ensureNotMaintenance(): Unit = {
    if (isMaintenanceMode) {
      throw new MaintenanceException("The system is currently in maintenance mode.\n" +
          "Please try again later.")
    }
  }

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
      throw wrapException(e)      
  }

  protected def cleanMaintenance[T](task: => T): T = try {
    task
  } catch {
    case e: Exception =>
    afterTaskCleanup()
    TaskRunner.reset()
    throw wrapException(e)
  }
  
  private def wrapException(e: Exception) = e match {
    case m: MaintenanceException => m
    case _ => new MaintenanceException(e)
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
