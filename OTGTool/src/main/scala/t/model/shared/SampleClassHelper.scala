package t.model.shared

import scala.collection.JavaConversions._
import t.model.SampleClass;

object SampleClassHelper {
  def apply(constraints: Map[String, String] = Map()): SampleClass =
    new SampleClass(constraints)
}
