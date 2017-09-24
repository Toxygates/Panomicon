package t.model.shared

import scala.collection.JavaConversions._
import t.model.SampleClass;
import t.model.sample.Attribute

object SampleClassHelper {
  def apply(constraints: Map[Attribute, String] = Map()): SampleClass =
    new SampleClass(constraints)
}
