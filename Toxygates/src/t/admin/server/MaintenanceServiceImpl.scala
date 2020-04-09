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

package t.admin.server

import scala.sys.process.Process
import t.{AffymetrixPlatform, BioPlatform, GeneralPlatform, PlatformFormat, PlatformManager, Task, TaskRunner}
import t.admin.client.MaintenanceService
import t.admin.shared.PlatformType
import t.common.server.maintenance.BatchOpsImpl
import t.common.shared.Dataset
import t.common.shared.ManagedItem
import t.common.shared.Platform
import t.common.shared.maintenance._
import t.common.shared.maintenance.Instance
import t.common.shared.maintenance.MaintenanceConstants._
import t.sparql.Datasets
import t.sparql.Instances
import t.sparql.Platforms
import t.sparql.ProbeStore
import t.sparql.TRDF
import t.util.TempFiles
import t.viewer.server.Configuration
import t.viewer.server.SharedDatasets
import t.viewer.server.rpc.TServiceServlet
import javax.servlet.http.HttpSession

abstract class MaintenanceServiceImpl extends TServiceServlet
with BatchOpsImpl with MaintenanceService {

  private var homeDir: String = _

  override def localInit(config: Configuration) {
    super.localInit(config)
    populateAttributes(baseConfig)
    homeDir = config.webappHomeDir
  }

  override protected def getAttribute[T](name: String, session: HttpSession) =
    session.getAttribute(name).asInstanceOf[T]

  override protected def setAttribute(name: String, x: AnyRef, session: HttpSession): Unit =
    session.setAttribute(name, x)

  override protected def request = getThreadLocalRequest

  private def format(pt: PlatformType): PlatformFormat = {
    pt match {
      case PlatformType.Standard => GeneralPlatform
      case PlatformType.Affymetrix => AffymetrixPlatform
      case PlatformType.Biological => BioPlatform
      case _ => throw new Exception("This tool does not support uploading of the specified platform format.")
    }
  }

  def addPlatformAsync(p: Platform, pt: PlatformType): Unit = {
    ensureNotMaintenance()
    showUploadedFiles()
    grabRunner()
    val pm = new PlatformManager(context)

    maintenance {
      setLastTask("Add platform")
      val platformFile = getLatestSessionFileAsTemp(maintenanceUploads(), platformPrefix,
          platformPrefix, "dat")
      if (platformFile == None) {
        throw new MaintenanceException("The platform file has not been uploaded yet.")
      }

      val id = p.getId
      val comment = p.getComment

      if (!TRDF.isValidIdentifier(id)) {
        throw new MaintenanceException(
          s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
      }

      runTasks(pm.add(id, TRDF.escape(comment),
          platformFile.get.getAbsolutePath(), format(pt)) andThen
          Task.simple("Set platform parameters"){ updatePlatform(p) })
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

    val id = i.getId()
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
    maintenance {
      setLastTask("Delete platform")
      runTasks(pm.delete(id))
    }
  }

  def delete(i: ManagedItem): Unit = {
    ensureNotMaintenance()
    i match {
      case i: Instance => deleteInstance(i.getId)
      case d: Dataset => deleteDataset(d.getId)
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
    val prs = new ProbeStore(baseConfig.triplestore)
    val np = prs.numProbes()
    val ps = new Platforms(baseConfig)
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
    val pfs = new Platforms(baseConfig)
    pfs.setComment(p.getId, p.getComment)
    pfs.setPublicComment(p.getId, p.getPublicComment)
  }

  private def updateInstance(i: Instance): Unit = {
    val is = new Instances(baseConfig.triplestore)
    is.setComment(i.getId, TRDF.escape(i.getComment))
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
