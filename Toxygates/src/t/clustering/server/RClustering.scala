/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import org.rosuda.REngine.Rserve.RserveException
import t.clustering.shared.Algorithm
import java.util.logging.Logger
import org.rosuda.REngine.Rserve.RserveException

/**
 * Connects to Rserve to perform a clustering.
 * @param codeDir the root directory where R/InCHlibUtils.R is available.
 */
class RClustering(codeDir: String) {
  val logger = Logger.getLogger("RClustering")

  //Types are Array rather than Seq for easy interop with Java

  /**
   * Perform the clustering and return the clusters as JSON data.
   *
   * @param data Column-major data (as a single sequence)
   * @param rowNames Row names (such as affymetrix probes)
   * @param colNames Column names
   * @param geneSyms Gene symbols for each row
   * @param algorithm The clustering algorithm to use
   * @param featureDecimalDigits the number of digits after the decimal point to retain in features
   */
  @throws(classOf[RserveException])
  def clustering(data: Array[Double], rowNames: Array[String],
      colNames: Array[String], geneSyms: Array[String],
      algorithm: Algorithm = new Algorithm(),
      featureDecimalDigits: Int = -1): String = {
    assert(data.length == rowNames.length * colNames.length)

    val r = new R
    r.addCommand(s"source('$codeDir/R/InCHlibUtils.R')")
    r.addCommand(s"data <- c(${data.mkString(", ")})")
    r.addCommand(s"r <- c(${rowNames.map { "\"" + _ + "\"" }.mkString(", ")})")
    r.addCommand(s"c <- c(${colNames.map { "\"" + _ + "\"" }.mkString(", ")})")
    r.addCommand("rowMethod <- \"" + algorithm.getRowMethod.asParam() + "\"")
    r.addCommand("rowDistance <- \"" + algorithm.getRowDistance.asParam() + "\"")
    r.addCommand("colMethod <- \"" + algorithm.getColMethod.asParam() + "\"")
    r.addCommand("colDistance <- \"" + algorithm.getColDistance.asParam() + "\"")
    r.addCommand("appendixes <- list(" + (rowNames zip geneSyms).map{ x =>
      s""""${x._1}"="${x._2}"""" }.mkString(",")   + ")")

    r.addCommand(s"getClusterAsJSON(data, r, c, rowMethod, rowDistance, colMethod, colDistance, appendixes, $featureDecimalDigits)")

    r.exec() match {
      case Some(x) => x.asString()
      case None => ""
    }
  }
}
