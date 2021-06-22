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

package t.viewer.server.rpc

import javax.servlet.http.HttpSession
import t.common.server.maintenance.BatchOpsImpl
import t.common.shared.Dataset
import t.common.shared.maintenance.{Batch, MaintenanceException}
import t.db.Metadata
import t.model.sample.Attribute
import t.model.sample.CoreParameter.{Platform, Treatment}
import t.model.sample.OTGAttribute.{Compound, DoseLevel, ExposureTime, Organ, Organism, Repeat, TestType}
import t.viewer.client.rpc.UserDataService
import t.viewer.server.Configuration

/**
 * A servlet for managing user data (as batches).
 * In practice, this is a restricted variant of the maintenanc
 * servlet.
 */
class UserDataServiceImpl extends TServiceServlet with BatchOpsImpl with UserDataService {
  private var homeDir: String = _
  private var instanceUri: String = _

  override def localInit(config: Configuration) {
    super.localInit(config)
    populateAttributes(baseConfig)
    instanceUri = config.instanceURI.get
    homeDir = config.webappHomeDir
  }

  override protected def getAttribute[T](name: String, session: HttpSession) =
    session.getAttribute(name).asInstanceOf[T]

  override protected def setAttribute(name: String, x: AnyRef, session: HttpSession): Unit =
    session.setAttribute(name, x)

  override protected def request = getThreadLocalRequest

  protected override def mayAppendBatch: Boolean = false

  //Public entry point
  override def addBatchAsync(b: Batch): Unit = {
    ensureNotMaintenance()
    checkAccess(b)
    //Here, we must first ensure existence of the dataset.
    //For user data, the unique user id will here be supplied from the client side.
    //(e.g. user-a1f8032011c0f...)
    //can also be user-shared in the case of shared user data.
    ensureDataset(b.getDataset)

    super.addBatchAsync(b)
  }

  //Public entry point
  override def updateBatchMetadataAsync(b: Batch, recalculate: Boolean): Unit = {
    ensureNotMaintenance()
    checkAccess(b)
    //See note about ensureDataset in addBatchAsync
    ensureDataset(b.getDataset)

    super.updateBatchMetadataAsync(b, recalculate)
  }

  //Currently not used - kept here for reference
  override protected def alterMetadataPriorToInsert(md: Metadata): Metadata = {
    //Enforce a special suffix for user data
    md.mapParameter(factory, "compound_name", n => {
      if (n.endsWith("[user]")) { n } else { s"$n [user]" }
    })
  }

  //Public entry point
  override def deleteBatchAsync(b: Batch): Unit = {
    ensureNotMaintenance()
    checkAccess(b)
    super.deleteBatchAsync(b)
  }

  //Public entry point
  def getBatches(datasets: Array[String]): Array[Batch] = {
    if (datasets == null || datasets.isEmpty) {
      //Security check - don't list batches unless they have the keys
      throw new MaintenanceException(
          "In the user data service, datasets must be specified explicitly.")
    }
    super.getBatches(datasets, Some(instanceUri))
  }

  //Indirectly called by update(ManagedItem) which is public
  override protected def updateBatch(b: Batch): Unit = {
    checkAccess(b)
    ensureDataset(b.getDataset)
    super.updateBatch(b)
  }

  //Public entry point
  override def batchAttributeSummary(b: Batch): Array[Array[String]] = {
    checkAccess(b)
    super.batchAttributeSummary(b)
  }

  private def checkAccess(b: Batch) {
    if (!Dataset.isUserDataset(b.getDataset)) {
      throw new MaintenanceException("Access not permitted.")
    }
  }

  private def ensureDataset(ds: String): Unit = {
    val desc = if (Dataset.isSharedDataset(ds)) "User data (public)" else "My data"
     val d = new Dataset(ds, desc, "Auto-generated", null, "Auto-generated", 0)
    addDataset(d, false)
  }

  //See MaintenanceServiceServlet
  //Note: might want to factor this out
  override protected def overviewParameters: Seq[Attribute] = {
    Seq(Organism, TestType, Repeat, Organ, Compound, DoseLevel, ExposureTime,
      Platform, Treatment)
  }

  override protected def checkMetadata(md: Metadata): Unit = {
    super.checkMetadata(md)
    //See OTGSeries.enums.
    //May create new: organ ID, compound name, exposure time
    val mayNotCreateNew = Seq(Repeat, TestType, Organism,
      DoseLevel)

    val enums = context.matrix.enumMaps
    for (p <- mayNotCreateNew) {
      val existing = enums(p.id).keySet
      md.attributeValues(p).find(!existing.contains(_)) match {
        case Some(v) =>
          throw new MaintenanceException(s"Metadata error: the value $v is unknown for parameter $p.")
        case None =>
      }
    }

    val pfs = md.attributeValues(Platform)
    pfs.find(!context.probeStore.platformsAndProbes.keySet.contains(_)) match {
      case Some(pf) =>
        throw new MaintenanceException(s"Metadata error: the platform_id $pf is unknown.")
      case None =>
    }

    try {
      //Note: consider how we handle new time points, test
      val timeUnits = md.attributeValues(ExposureTime).map(_.split(" ")(1))
      println(s"timeUnits: $timeUnits")
      val accepted = Seq("hr", "day")
      timeUnits.find(!accepted.contains(_)) match {
        case Some(v) =>
          throw new MaintenanceException(s"Metadata error: the unit $v is unacceptable for exposure_time")
        case None =>
      }
    } catch {
      case e: Exception =>
        throw new MaintenanceException("Metadata error: couldn't parse exposure_time")
    }
  }

  /**
   * Generate a new user key, to be used when the client does not already have one.
   */
  def newUserKey(): String = {
    val time = System.currentTimeMillis()
    val random = (Math.random * Int.MaxValue).toInt
    "%x%x".format(time, random)
  }
}
