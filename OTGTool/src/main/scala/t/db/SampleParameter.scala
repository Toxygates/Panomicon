package t.db

import t.model.sample.Attribute

case class SampleParameter(identifier: String, humanReadable: String) extends Attribute {
  final def id: String = identifier

  def title = humanReadable

  def isNumerical = false
}

/**
 * Some core sample parameters that must be present (a refactoring in progress)
 */
object SampleParameters {
  val ControlGroup = SampleParameter("control_group", "Control group")
  val ExposureTime =
    SampleParameter(t.model.sample.CoreParameter.ExposureTime.id,
        t.model.sample.CoreParameter.ExposureTime.title())
  val DoseLevel = SampleParameter("dose_level", "Dose level")
  val BatchGraph = SampleParameter("batchGraph", "Batch")
  val Individual = SampleParameter("individual_id", "Individual ID")
}
