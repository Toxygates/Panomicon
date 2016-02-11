package otgviewer.server.rpc

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
}
