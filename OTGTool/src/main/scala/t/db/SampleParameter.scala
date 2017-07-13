package t.db

case class SampleParameter(identifier: String, humanReadable: String) {
  final def id: String = identifier
}

/**
 * Some core sample parameters that must be present (a refactoring in progress)
 */
object SampleParameters {
  val ControlGroup = SampleParameter("control_group", "Control group")
  val ExposureTime =
    SampleParameter(t.model.SampleParameter.ExposureTime.id,
        t.model.SampleParameter.ExposureTime.title())
  val DoseLevel = SampleParameter("dose_level", "Dose level")
  val BatchGraph = SampleParameter("batchGraph", "Batch")
  val Individual = SampleParameter("individual_id", "Individual ID")
}
