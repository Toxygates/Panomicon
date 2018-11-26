package t.viewer.server.network

import t.viewer.server.matrix.MatrixController
import t.Context
import t.common.shared.ValueType
import t.common.shared.sample.Group
import t.viewer.server.matrix.ManagedMatrix
import t.platform.mirna.TargetTable
import t.viewer.server.Platforms
import t.viewer.server.matrix.ControllerParams
import t.viewer.shared.network.Network

/**
 * A MatrixController that turns the main matrix into a ManagedNetwork
 * instead of a ManagedMatrix.
 */
class NetworkController(params: ControllerParams,  
    val sideMatrix: ManagedMatrix, targets: TargetTable,
    platforms: Platforms,
    initMainPageSize: Int,
    sideIsMRNA: Boolean) extends MatrixController(params) {

  type Mat = ManagedNetwork
  
  override def finish(mm: ManagedMatrix): Mat = {
    new ManagedNetwork(mm.params, sideMatrix, targets, platforms, initMainPageSize, sideIsMRNA)
  }

  /**
   * Produce a network object that reflects the current view.
   */
  def makeNetwork: Network =
    new NetworkBuilder(targets, platforms, managedMatrix, sideMatrix).build

}
