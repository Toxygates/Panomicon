/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import scala.collection.JavaConverters._

import org.apache.commons.fileupload.FileItem

import gwtupload.server.UploadServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession
import t.Task
import t.TaskRunner
import t.common.shared.maintenance.MaintenanceException
import t.common.shared.maintenance.OperationResults
import t.common.shared.maintenance.Progress
import t.global.KCDBRegistry
import t.util.TempFiles
import t.viewer.server.rpc.TServiceServlet

/**
 * Servlet routines for uploading files and running tasks.
 */
trait MaintenanceOpsImpl extends t.common.client.rpc.MaintenanceOperations {
  this: TServiceServlet =>

  //Methods for accessing the http request's thread local state.
  //Must be abstract here or an access error will be thrown at runtime.
  //(Implement in the concrete subclass)
  protected def request: HttpServletRequest
  protected def getAttribute[T](name: String,
      session: HttpSession = request.getSession()): T
  protected def setAttribute(name: String, x: AnyRef,
      session: HttpSession = request.getSession()): Unit

  protected def setLastTask(task: String) = setAttribute("lastTask", task)
  protected def lastTask: String = getAttribute("lastTask")
  protected def setLastResults(results: Option[OperationResults]) = setAttribute("lastResults", results)
  protected def lastResults: Option[OperationResults] = getAttribute("lastResults")

  protected def maintenanceUploads(session: HttpSession = request.getSession()): TempFiles = {
    val existingTempFiles: Option[TempFiles] = Option(getAttribute("maintenanceUploads", session))
    if (existingTempFiles.isEmpty) {
      val newTempFiles = new TempFiles()
      setAttribute("maintenanceUploads", newTempFiles, session)
      newTempFiles
    } else {
      existingTempFiles.get
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

  def getOperationResults(): OperationResults = {
    if (lastResults.isEmpty) {
      throw new MaintenanceException("Cannot get operation results: operation not yet complete")
    }
    lastResults.get
  }

  def cancelTask(): Unit = {
    TaskRunner.log("* * * Cancel requested * * *")
    TaskRunner.shutdown()
  }

  def getProgress(): Progress = {
    TaskRunner.synchronized {
      val messages = TaskRunner.logMessages.toArray
      val p = if (TaskRunner.available) {
        new Progress("No task in progress", 0, true)
      } else {
        TaskRunner.currentAtomicTask match {
          case Some(t) => new Progress(t.name, t.percentComplete, false)
          case None => new Progress("??", 0, false)
        }
      }
      p.setMessages(messages)
      p
    }
  }

  protected def grabRunner() {
    if (!TaskRunner.available) {
      throw new MaintenanceException("Another task is already in progress.")
    }
  }

  protected def runTasks(task: Task[_]) {
    grabRunner()
    setLastResults(None)
    val currentRequest = request
    val session = request.getSession
    TaskRunner.runThenFinally(task) {
      KCDBRegistry.closeWriters()
      TaskRunner.synchronized {
        try {
          val success = TaskRunner.errorCause == None
          if (getAttribute[Option[OperationResults]]("lastResults", session).isEmpty) {
            setAttribute("lastResults", Some(new OperationResults(
                getAttribute[String]("lastTask", session), success, TaskRunner.resultMessages.toArray)), session)
          }
          if (success) {
            maintenanceUploads(session).dropAll()
          }
        } catch {
          case e: Exception =>
            println(e)
            e.printStackTrace()
        }
      }
    }
  }

  protected def maintenance[T](task: => T): T = try {
    task
  } catch {
    case e: Exception =>
      throw wrapException(e)
  }

  private def wrapException(e: Exception) = e match {
    case m: MaintenanceException => m
    case _ => new MaintenanceException(e)
  }

  /**
   * Gets the latest file uploaded to the session with matching prefix/suffix or,
   * failing that, the matching file in tempFiles if one exists.
   */
  protected def getLatestFile(tempFiles: TempFiles, tag: String, prefix: String,
      suffix: String): Option[java.io.File] = {
    getLatestSessionFileAsTemp(tempFiles, tag, prefix, suffix) orElse
      tempFiles.getExisting(prefix, suffix)
  }

  /**
   * Get latest file uploaded to session with matching prefix/suffix as a temporary file.
   * Should be deleted after use.
   */
  protected def getLatestSessionFileAsTemp(tempFiles: TempFiles, tag: String, prefix: String,
      suffix: String): Option[java.io.File] = {
    getFileItem(tag) match {
      case None => None
      case Some(fi) =>
        val f = tempFiles.makeNew(prefix, suffix)
        fi.write(f)
        println(s"Deleting ${fi.getFieldName()}")
        fi.delete()
        Some(f)
    }
  }

   /**
   * Retrive the last uploaded file with a particular tag, and delete it,
   * along with all other files with the same tag.
   * @param tag the tag to look for.
   * @return
   */
  // TODO: stop relying on undocumented UploadServlet.getSessionFileItems sort order
  private def getFileItem(tag: String): Option[FileItem] = {
    val sessionItems = UploadServlet.getSessionFileItems(request);
    if (sessionItems == null) {
      throw new MaintenanceException("No files have been uploaded yet.")
    }
    // We are currently relying on getSessionFileItems being sorted in ascending
    // order of upload time, which is undocumented behavior.
    val tagItems = sessionItems.asScala.filter(_.getFieldName().startsWith(tag)).reverse
    // Session files get deleted when their corresponding FileItems are garbage
    // collected, so it's enough to remove them from the session file list
    tagItems.foreach(sessionItems.remove(_))
    return tagItems.lift(0)
  }

  protected def showUploadedFiles(): Unit = {
    println("Session file items:")
    val items = UploadServlet.getSessionFileItems(request)
    if (items != null) {
      for (fi <- items.asScala) {
        println(s"${fi.getName}  size ${fi.getSize}  field: ${fi.getFieldName}")
      }
    }
    println("Maintenace uploads:")
    maintenanceUploads().registry.foreach({ case ((prefix, suffix), file) =>
      println(s"${file.getName} size ${file.length} prefix $prefix suffix $suffix")
    })
  }
}
