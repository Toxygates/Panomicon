package t.viewer.server.network

import t.db._
import t.viewer.server.matrix._
import t.viewer.shared.ManagedMatrixInfo
import t.platform.mirna._
import t.viewer.server.rpc.NetworkState
import t.viewer.shared.network.NetworkInfo
import t.viewer.shared.network.Network
import t.viewer.server.Platforms
import scala.collection.JavaConverters._
import t.common.shared.sample.ExpressionRow

/**
 * Extended version of ManagedMatrix to preserve
 * the relationship between the main and the side matrices in a network,
 * when sorting, filtering, etc. happens to the former.
 * Each time a new page of the main matrix is displayed, or when any of the above changes happens,
 * the side matrix is updated accordingly.
 *
 * For best performance, the target table should be kept as small as possible
 * (i.e. pre-filtered for species, platform etc)
 */
class ManagedNetwork(mainParams: LoadParams,
    val sideMatrix: ManagedMatrix,
    var targets: TargetTable,
    platforms: Platforms,
    var currentPageSize: Int,
    sideIsMRNA: Boolean) extends ManagedMatrix(mainParams) {

  protected var currentPageRows: Option[(Int, Int)] = None

  override def getPageView(offset: Int, length: Int): Seq[ExpressionRow] = {
    val r = super.getPageView(offset, length)
    currentPageRows = Some((offset, r.size))
    updateSideMatrix()
    r
  }

  /**
   * To be called when the superclass' current view has changed.
   * Obtains the relevant rows for and reloads the side matrix
   * accordingly.
   */
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
    if (targets.isEmpty) {
      Console.err.println("Warning: unable to build count map, targets table is empty")
    }
    val r = buildCountMap(mat)
    if (sideMatrix.initProbes.isEmpty) {
      Console.err.println("Warning: unable to build count map, initProbes is empty")
    }
    val pset = sideMatrix.initProbes.toSet
    r.filter(x => pset.contains(x._1))
  }

  def countMap = filteredCountMap(rawGrouped)

  import java.util.{HashMap => JHMap}
  import java.lang.{Double => JDouble}

  // THe use of JHMap ensures that this map is ready for GWT serialisation.
  private[this] var currentCountMap: JHMap[ProbeId, JDouble] =
    new JHMap[ProbeId, JDouble]

  /**
   * A mutable count map that will be updated as the current gene set changes,
   * to reflect the counts in that set (counting the number of times each
   * miRNA occurs in a mRNA table, or vice versa)
   * This map is GWT-serialisable.
   */
  def currentViewCountMap: JHMap[ProbeId, JDouble] = currentCountMap

  def buildCountMap(mat: ExprMatrix): Map[String, JDouble] = {
    val lookup = mat.rowKeys.toSeq

    if (sideIsMRNA) {
      val all = platforms.data(sideMatrix.params.platform)
      val rowTargets = targets.targets(lookup.map(MiRNA(_)), all)
      rowTargets.groupBy(_._2).map(x => (x._1.identifier, new JDouble(x._2.size)))
    } else {
      val resolved = platforms.resolve(lookup)
      val rowTargets = targets.reverseTargets(resolved)
      rowTargets.groupBy(_._2).map(x => (x._1.id, new JDouble(x._2.size)))
    }
  }

  override private[server] def currentRowsChanged() = synchronized {
    super.currentRowsChanged
    if (currentCountMap != null) {
      currentCountMap.clear()
      currentCountMap.putAll(filteredCountMap(current).asJava)
    }
  }
}
