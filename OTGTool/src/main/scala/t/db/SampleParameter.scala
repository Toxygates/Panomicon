package t.db

import t.model.sample.Attribute

@deprecated("Being replaced with Attribute", "Aug 2017")
case class SampleParameter(identifier: String, humanReadable: String) extends Attribute {
  final def id: String = identifier

  def title = humanReadable

  def isNumerical = false
}
