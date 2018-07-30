package t.platform

package object mirna {
  /**
   * Example: hsa-let-7a-2-3p
   */
  case class MiRNA(id: String) extends AnyVal {
    def asProbe = Probe(id)
  }
}
