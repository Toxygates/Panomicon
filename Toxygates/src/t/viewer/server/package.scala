package t.viewer

import t.common.shared.AType
import t.sparql._
import t.platform.Probe
import t.model.SampleClass

package object server {
  /**
   * An association resolver partial function defined for some combinations of association types
   * and probes.
   * Different AssociationLookups are chained together to form the final resolution mechanism.
   */
  type AssociationLookup = PartialFunction[(AType, SampleClass, SampleFilter, Iterable[Probe]), BBMap]
}