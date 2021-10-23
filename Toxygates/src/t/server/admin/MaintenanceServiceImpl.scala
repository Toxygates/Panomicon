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

package t.server.admin

import javax.servlet.http.HttpSession
import t.gwt.admin.client.MaintenanceService
import t.shared.admin.PlatformType
import t.server.common.maintenance.BatchOpsImpl
import t.shared.common.maintenance.MaintenanceConstants._
import t.shared.common.maintenance.{Instance, _}
import t.shared.common.{Dataset, ManagedItem, Platform}
import t.manager.{PlatformManager, Task}
import t.platform.{AffymetrixPlatform, BioPlatform, GeneralPlatform, PlatformFormat}
import t.sparql.{DatasetStore, InstanceStore, PlatformStore, ProbeStore, TRDF}
import t.server.viewer.rpc.{TServiceServlet}
import t.server.viewer.{Configuration, SharedDatasets}

import scala.sys.process.Process

class MaintenanceServiceImpl extends TServiceServlet
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

  private def format(platformType: PlatformType): PlatformFormat = {
    platformType match {
      case PlatformType.Standard => GeneralPlatform
      case PlatformType.Affymetrix => AffymetrixPlatform
      case PlatformType.Biological => BioPlatform
      case _ => throw new Exception("This tool does not support uploading of the specified platform format.")
    }
  }

  def addPlatformAsync(platform: Platform, platformType: PlatformType): Unit = {
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

      val id = platform.getId
      val comment = platform.getComment

      if (!TRDF.isValidIdentifier(id)) {
        throw new MaintenanceException(
          s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
      }

      runTasks(pm.add(id, TRDF.escape(comment),
          platformFile.get.getAbsolutePath(), format(platformType)) andThen
          Task.simple("Set platform parameters"){ updatePlatform(platform) })
    }
  }

  def add(item: ManagedItem): Unit = {
    ensureNotMaintenance()
    item match {
      case d: Dataset => addDataset(d, true)
      case i: Instance => addInstance(i)
      case _ => throw new MaintenanceException(s"Illegal API usage, cannot add $item")
    }
  }

  private def addInstance(instance: Instance): Unit = {
    val im = new InstanceStore(baseConfig.triplestoreConfig)

    val id = instance.getId()
    val param = instance.getPolicyParameter()
    if (im.getList().contains(id)) {
      throw new MaintenanceException(s"The instance $id already exists, please choose a different name")
    }

    maintenance {
      val ap = instance.getAccessPolicy()
      val policy = ap.toString().toLowerCase();
      val cmd = s"sh $homeDir/new_instance.${policy}.sh $id $id $param"
      println(s"To finish adding the new instance, please run : $cmd")
      im.addWithTimestamp(id, TRDF.escape(instance.getComment))
    }
  }

  def getBatches(batchIds: Array[String]) = super.getBatches(batchIds, None)

  def deletePlatformAsync(id: String): Unit = {
    ensureNotMaintenance()
    grabRunner()
    val pm = new PlatformManager(context)
    maintenance {
      setLastTask("Delete platform")
      runTasks(pm.delete(id))
    }
  }

  def delete(item: ManagedItem): Unit = {
    ensureNotMaintenance()
    item match {
      case i: Instance => deleteInstance(i.getId)
      case d: Dataset => deleteDataset(d.getId)
      case _ => throw new MaintenanceException("Illegal API usage")
    }
  }

  private def deleteInstance(id: String): Unit = {
    val im = new InstanceStore(baseConfig.triplestoreConfig)
    maintenance {
      val cmd = s"sh $homeDir/delete_instance.sh $id $id"
      println(s"To finish deleting the instance, please run: $cmd")
      im.delete(id)
    }
  }

  private def deleteDataset(id: String): Unit = {
    val dm = new DatasetStore(baseConfig.triplestoreConfig)
    maintenance {
      dm.delete(id)
    }
  }

  def getPlatforms: Array[Platform] = {
    val prs = new ProbeStore(baseConfig.triplestoreConfig)
    val np = prs.numProbes()
    val ps = new PlatformStore(baseConfig)
    val comments = ps.getComments()
    val pubComments = ps.getPublicComments()
    val dates = ps.getTimestamps()
    ps.getList().map(p => {
      new Platform(p, np.getOrElse(p, 0), comments.getOrElse(p, ""),
          dates.getOrElse(p, null), pubComments.getOrElse(p, ""))
    }).toArray
  }

  def getInstances: Array[Instance] = {
    val is = new InstanceStore(baseConfig.triplestoreConfig)
    val com = is.getComments()
    val ts = is.getTimestamps()
    is.getList().map(i => new Instance(i, com.getOrElse(i, ""),
        ts.getOrElse(i, null))).toArray
  }

  def getDatasets: Array[Dataset] = {
    val ds = new DatasetStore(baseConfig.triplestoreConfig) with SharedDatasets
    ds.sharedList(None).toArray
  }

  private def updatePlatform(platform: Platform): Unit = {
    val pfs = new PlatformStore(baseConfig)
    pfs.setComment(platform.getId, platform.getComment)
    pfs.setPublicComment(platform.getId, platform.getPublicComment)
  }

  private def updateInstance(instance: Instance): Unit = {
    val is = new InstanceStore(baseConfig.triplestoreConfig)
    is.setComment(instance.getId, TRDF.escape(instance.getComment))
  }

  override def update(item: ManagedItem): Unit = {
    ensureNotMaintenance()
    item match {
      case i: Instance => updateInstance(i)
      case p: Platform => updatePlatform(p)
      case d: Dataset => updateDataset(d)
      case _ => super.update(item)
    }
  }
}
