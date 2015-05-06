package t

import otg.sparql.OTGSamples
import t.db.Metadata
import t.db.Sample
import t.sparql.Samples

abstract class ValueSummary(valueName: String) {
  def originalValues: Set[String]

  var newValues: Set[String] = Set()
  var existingValues: Set[String] = Set()

  def check(x: String) {
    if (originalValues.contains(x)) {
      existingValues += x
    } else {
      newValues += x
    }
  }

  def summary(detail: Boolean): String = {
    var r = valueName + ": " + existingValues.size + " existing values, " +
      newValues.size + " new values"
    if (detail && newValues.size > 0) {
      r += ": " + newValues.mkString(" ")
    }
    r
  }
}

class SimpleValueSummary(valueName: String, val originalValues: Set[String]) 
	extends ValueSummary(valueName)

case class AttribValueSummary(samples: Samples, attribName: String)
  extends ValueSummary(attribName) {
  val originalValues = samples.allValuesForSampleAttribute(attribName).toSet

  def check(md: Metadata, samples: Iterable[Sample]) {
    for (
      s <- samples; a <- md.parameters(s);
      if (a._1.identifier == attribName)
    ) {
      check(a._2)
    }
  }
}