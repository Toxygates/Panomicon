package t.platform

package object mirna {
  /**
   * Example: hsa-let-7a-2-3p
   */
  case class MiRNA(id: String) extends AnyVal {
    def asProbe = Probe(id)
  }

  type DatabaseID = String

  type Interaction = (MiRNA, RefSeq, Double, DatabaseID)

  def isMiRNAProbe(probe: Probe): Boolean = {
    probe.identifier.contains("-miR-") ||
      probe.identifier.contains("-let-")
  }
}
