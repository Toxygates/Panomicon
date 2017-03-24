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

package t.admin.server

import java.util.HashSet

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.setAsJavaSet
import scala.sys.process.Process

import org.apache.commons.fileupload.FileItem

import gwtupload.server.UploadServlet
import t.BatchManager
import t.PlatformManager
import t.TaskRunner
import t.Tasklet
import t.admin.client.MaintenanceService
import t.common.shared.maintenance.Batch
import t.common.shared.maintenance.Instance
import t.common.shared.maintenance._
import t.common.shared.maintenance.MaintenanceConstants._
import t.common.shared.Dataset
import t.common.shared.ManagedItem
import t.common.shared.Platform
import t.sparql.Batches
import t.sparql.Datasets
import t.sparql.Instances
import t.sparql.Platforms
import t.sparql.Probes
import t.sparql.SampleFilter
import t.sparql.TRDF
import t.util.TempFiles
import t.viewer.server.Configuration
import t.viewer.server.SharedDatasets
import t.viewer.server.rpc.TServiceServlet
import t.common.server.maintenance.BatchOpsImpl

abstract class MaintenanceServiceImpl extends TServiceServlet
with BatchOpsImpl with MaintenanceService {

  private var homeDir: String = _

  override def localInit(config: Configuration) {
    super.localInit(config)
    homeDir = config.webappHomeDir
  }

  override protected def getAttribute[T](name: String) =
    getThreadLocalRequest().getSession().getAttribute(name).asInstanceOf[T]

  override protected def setAttribute(name: String, x: AnyRef): Unit =
     getThreadLocalRequest().getSession().setAttribute(name, x)

  override protected def request = getThreadLocalRequest

  def addPlatformAsync(p: Platform, affymetrixFormat: Boolean): Unit = {
    ensureNotMaintenance()
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
      TaskRunner ++= pm.add(id, TRDF.escape(comment),
          metaFile.getAbsolutePath(), affymetrixFormat)
      TaskRunner += Tasklet.simple("Set platform parameters", () => updatePlatform(p))
    }
  }

  def add(i: ManagedItem): Unit = {
    ensureNotMaintenance()
    i match {
      case d: Dataset => addDataset(d, true)
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
      im.addWithTimestamp(id, TRDF.escape(i.getComment))
      if (p != 0) {
        throw new MaintenanceException(s"Tomcat instance creation failed: return code $p. Please investigate manualy.")
      }
    }
  }

  def deletePlatformAsync(id: String): Unit = {
    ensureNotMaintenance()
    grabRunner()
    val pm = new PlatformManager(context)
    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Delete platform")
      TaskRunner ++= pm.delete(id)
    }
  }

  def delete(i: ManagedItem): Unit = {
    ensureNotMaintenance()
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
      im.delete(id)
      if (p != 0) {
        throw new MaintenanceException(s"Deleting tomcat instance failed: return code $p. Please investigate manually.")
      }

    }
  }

  private def deleteDataset(id: String): Unit = {
    val dm = new Datasets(baseConfig.triplestore)
    maintenance {
      dm.delete(id)
    }
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

  private def updatePlatform(p: Platform): Unit = {
    val pfs = new Platforms(baseConfig.triplestore)
    pfs.setComment(p.getTitle, p.getComment)
    pfs.setPublicComment(p.getTitle, p.getPublicComment)
  }

  private def updateInstance(i: Instance): Unit = {
    val is = new Instances(baseConfig.triplestore)
    is.setComment(i.getTitle, TRDF.escape(i.getComment))
  }

  override def update(i: ManagedItem): Unit = {
    ensureNotMaintenance()
    i match {
      case i: Instance => updateInstance(i)
      case p: Platform => updatePlatform(p)
      case d: Dataset => updateDataset(d)
      case _ => super.update(i)
    }
  }
}
