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
import scala.collection.JavaConverters._

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
    var currentPageSize: Int,
    sideIsMRNA: Boolean) extends ManagedMatrix(mainParams) {

  //TODO check if this is called in the correct places
  override protected def currentViewChanged() {
    super.currentViewChanged()
    updateSideMatrix()
  }

  def updateSideMatrix() {
    val offset = currentPageRows.map(_._1).getOrElse(0)
    val length = currentPageRows.map(_._2).getOrElse(currentPageSize)
    if (targets.size == 0) {
      println("Warning: targets table is empty. No side table can be constructed.")
    }
    val sideProbes = NetworkBuilder.extractSideProbes(targets, platforms,
        this, sideMatrix, offset, length)
    println(s"Managed network: selecting ${sideProbes.size} probes for side matrix")
    sideMatrix.selectProbes(sideProbes)
  }

  private def filteredCountMap(mat: ExprMatrix) = {
    val sidePlatform = sideMatrix.params.platform
    val r = NetworkState.buildCountMap(currentInfo, mat, targets, platforms,
      sidePlatform, sideIsMRNA)
    val pset = sideMatrix.initProbes.toSet
    r.filter(x => pset.contains(x._1))
  }

  def countMap = filteredCountMap(rawGrouped)

  import java.util.{HashMap => JHMap}
  import java.lang.{Double => JDouble}

  private[this] var currentCountMap: JHMap[String, JDouble] = new JHMap[String, JDouble]

  /**
   * A mutable count map that will be updated as the current gene set changes,
   * to reflect the counts in that set.
   */
  def currentViewCountMap: JHMap[String, JDouble] = currentCountMap

  override protected def updateRowInfo() = synchronized {
    super.updateRowInfo
    if (currentCountMap != null) {
      currentCountMap.clear()
      currentCountMap.putAll(filteredCountMap(current).asJava)
    }
  }
}
