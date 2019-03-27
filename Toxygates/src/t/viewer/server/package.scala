package t.viewer

import t.common.shared.AType
import t.sparql._
import t.platform.Probe
import t.model.SampleClass

package object server {
  /**
   * An association resolver function defined for some combinations of association types
   * and probes.
   */
  type AssociationLookup = PartialFunction[(AType, SampleClass, SampleFilter, Iterable[Probe]), BBMap]
}