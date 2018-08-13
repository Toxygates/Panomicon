package t.viewer.server.network

import t.viewer.server.matrix.ManagedMatrix
import t.viewer.server.matrix.ExprMatrix
import t.viewer.shared.ManagedMatrixInfo
import t.platform.mirna.TargetTable
import t.viewer.server.matrix.LoadParams
import t.viewer.server.rpc.NetworkState
import t.viewer.server.matrix.MatrixController
import t.viewer.shared.network.NetworkInfo
import t.viewer.shared.network.Network
import t.viewer.server.Platforms

/**
 * Extended version of ManagedMatrix to preserve
 * the relationship between the main and the side matrices in a network,
 * when sorting, filtering, etc. happens to the former.
 *
 * For best performance, the target table should be pre-filtered for the species.
 */
class ManagedNetwork(mainParams: LoadParams,
    sideMatrix: ManagedMatrix,
    var targets: TargetTable,
    platforms: Platforms,
    var currentPageSize: Int) extends ManagedMatrix(mainParams) {

  //TODO check if this is called in the correct places
  override protected def currentViewChanged() {
    super.currentViewChanged()
    updateSideMatrix()
  }

  def updateSideMatrix() {    
    val offset = currentPageRows.map(_._1).getOrElse(0)
    val length = currentPageRows.map(_._2).getOrElse(currentPageSize)
    val sideProbes = NetworkBuilder.extractSideProbes(targets, platforms,
        this, offset, length)
    println(s"Managed network: selecting ${sideProbes.size} probes for side matrix")
    sideMatrix.selectProbes(sideProbes)
  }
}
