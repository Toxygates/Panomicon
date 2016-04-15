package otgviewer.server.rpc

import t.db.Metadata
import t.common.shared.maintenance.MaintenanceException

class UserDataServiceImpl extends t.viewer.server.rpc.UserDataServiceImpl
  with OTGServiceServlet {

  //See MaintenanceServiceServlet
  //TODO: factor out
  override protected def overviewParameters: Seq[t.db.SampleParameter] = {
    val r = Vector("organism", "test_type", "sin_rep_type", "organ_id",
        "compound_name", "dose_level", "exposure_time",
        "platform_id", "control_group")
    r.map(context.config.sampleParameters.byId)
  }

  override protected def checkMetadata(md: Metadata): Unit = {
    super.checkMetadata(md)
    //See OTGSeries.enums.
    //May create new: organ ID, compound name, exposure time
    val mayNotCreateNew = Seq("sin_rep_type", "test_type", "organism",
        "dose_level")

     val enums = context.matrix.enumMaps
     for (p <- mayNotCreateNew) {
       val existing = enums(p).keySet
       md.parameterValues(p).find(!existing.contains(_)) match {
         case Some(v) =>
           throw new MaintenanceException(s"Metadata error: the value $v is unknown for parameter $p.")
         case None =>
       }
     }

    val pfs = md.parameterValues("platform_id")
    pfs.find(!context.probes.platformsAndProbes.keySet.contains(_)) match {
      case Some(pf) =>
        throw new MaintenanceException(s"Metadata error: the platform_id $pf is unknown.")
      case None =>
    }

    try {
      //TODO consider how we handle new time points, test
      val timeUnits = md.parameterValues("exposure_time").map(_.split(" ")(1))
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
