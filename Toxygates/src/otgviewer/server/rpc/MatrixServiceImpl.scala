/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package otgviewer.server.rpc

import java.util.{ List => JList }
import otgviewer.server.rpc.Conversions.asScala
import t.common.shared.ValueType
import org.rosuda.REngine.Rserve.RserveException
import otgviewer.server.R
import t.viewer.server.Configuration
import java.util.logging.Logger
import otgviewer.shared.Group
import java.io.File
import javax.servlet.ServletContext

class MatrixServiceImpl extends t.viewer.server.rpc.MatrixServiceImpl
  with OTGServiceServlet {

    private val logger = Logger.getLogger("MatrixService")

    var userDir: String = null

    override def localInit(config: Configuration) {
      super.localInit(config)
      this.userDir = this.getServletContext.getRealPath("/WEB-INF/")
    }

    def prepareHeatmap(groups: JList[Group], chosenProbes: Array[String],
    valueType: ValueType): String = {

    loadMatrix(groups, chosenProbes, valueType)

    val mm = getSessionData.matrix
    var mat = mm.current
    var info = mm.info

    //TODO shared logic with e.g. insertAnnotations, extract
    val rowNames = mat.asRows.map(_.getAtomicProbes.mkString("/"))
    val columns = mat.sortedColumnMap.filter(x => !info.isPValueColumn(x._2))
    val colNames = columns.map(_._1)
    val values = columns.map(x => mat.data.map(_.map(asScala(_).value)).map{_(x._2)})

    clustering(values.flatten, rowNames, colNames)
  }

  @throws(classOf[RserveException])
  def clustering(data: Seq[Double], rowName: Seq[String], colName: Seq[String]) = {
    assert(data.length == rowName.length * colName.length)

    val r = new R
    logger.info(s"Read source file: $userDir/R/InCHlibUtils.R")
    r.addCommand(s"source('$userDir/R/InCHlibUtils.R')")
    r.addCommand(s"data <- c(${data.mkString(", ")})")
    r.addCommand(s"r <- c(${rowName.map{"\"" + _ + "\""}.mkString(", ")})")
    r.addCommand(s"c <- c(${colName.map{"\"" + _ + "\""}.mkString(", ")})")
    r.addCommand(s"getClusterAsJSON(data, r, c)")

    r.exec() match {
      case Some(x) => x.asString()
      case None    => ""
    }
  }

}
