/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

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
import t.common.shared.GWTTypes
import t.common.server.GWTUtils

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
    if (targets.isEmpty) {
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

  private def countMap = filteredCountMap(rawGrouped)

  import java.lang.{Double => JDouble}

  import GWTTypes._
  import GWTUtils._

  private[this] var currentCountMap = mkMap[ProbeId, JDouble]

  /**
   * A mutable count map that will be updated as the current gene set changes,
   * to reflect the counts in that set (counting the number of times each
   * miRNA occurs in a mRNA table, or vice versa)
   * This map is GWT-serializable.
   */
  def currentViewCountMap: GWTMap[ProbeId, JDouble] = currentCountMap

  def buildCountMap(mat: ExprMatrix): Map[String, JDouble] = {
    val lookup = mat.rowKeys.toSeq

    if (sideIsMRNA) {
      val all = platforms.data(sideMatrix.params.platform)
      val rowTargets = targets.targets(lookup.map(MiRNA), all)
      rowTargets.groupBy(_._2).map(x => {
        //the same association can occur through multiple mappings - count
        //distinct end to end mappings here
        val n = x._2.toSeq.distinct.size
        (x._1.identifier, new JDouble(n))
      })
    } else {
      val resolved = platforms.resolve(lookup)
      val rowTargets = targets.reverseTargets(resolved)
      rowTargets.groupBy(_._2).map(x => {
        //as above
        val n = x._2.toSeq.distinct.size
        (x._1.id, new JDouble(n))
      })
    }
  }

  override private[server] def currentRowsChanged(): Unit = synchronized {
    super.currentRowsChanged()
    if (currentCountMap != null) {
      currentCountMap.clear()
      currentCountMap.putAll(filteredCountMap(current).asJava)
    }
  }
}
