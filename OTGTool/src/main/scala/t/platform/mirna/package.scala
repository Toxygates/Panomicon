package t.platform

package object mirna {
  /**
   * Example: hsa-let-7a-2-3p
   */
  case class MiRNA(id: String) extends AnyVal {
    def asProbe = Probe(id)
  }

  type Interaction = (MiRNA, RefSeq, Double, TargetSourceInfo)

  def isMiRNAProbe(probe: Probe): Boolean = {
    probe.identifier.contains("-miR-") ||
      probe.identifier.contains("-let-")
  }
}
