/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otg.viewer.server.rpc

import t.common.shared.maintenance.MaintenanceException

import t.db.Metadata
import t.model.sample._
import t.model.sample.CoreParameter._
import otg.model.sample.OTGAttribute._

class UserDataServiceImpl extends t.viewer.server.rpc.UserDataServiceImpl
  with OTGServiceServlet {

  //See MaintenanceServiceServlet
  //Note: might want to factor this out
  override protected def overviewParameters: Seq[Attribute] = {
    Seq(Organism, TestType, Repeat, Organ, Compound, DoseLevel, ExposureTime,
      Platform, ControlGroup)
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
    pfs.find(!context.probes.platformsAndProbes.keySet.contains(_)) match {
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
}
