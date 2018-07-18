package t

package object platform {
  /**
   * A RefSeq transcript. Example ID: NM_014155
   */
  case class RefSeq(id: String) extends AnyVal
}