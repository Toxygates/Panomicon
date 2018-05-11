/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.clustering.server

import java.util.{List => JList}

import scala.collection.JavaConversions._

import com.google.gwt.user.server.rpc.RemoteServiceServlet



import t.clustering.client.ClusteringService
import t.clustering.shared.Algorithm

abstract class ClusteringServiceImpl[C, R] extends RemoteServiceServlet with ClusteringService[C, R] {

  def prepareHeatmap(columns: JList[C], rows: JList[R],
    algorithm: Algorithm): String = {

    val data = clusteringData(columns, rows)

    val clust = new RClustering(data.codeDir)

    clust.clustering(data.data.flatten, Array() ++ data.rowNames,
        Array() ++ data.colNames,
        Array() ++ data.geneSymbols, algorithm)
  }

  protected def clusteringData(cols: JList[C], rows: JList[R]): ClusteringData

  override def doUnexpectedFailure(t: Throwable) {
    t.printStackTrace()
    super.doUnexpectedFailure(t)
  }
}
