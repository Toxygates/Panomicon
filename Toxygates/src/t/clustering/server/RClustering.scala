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
package t.clustering.server

import java.util.logging.Logger
import org.rosuda.REngine.Rserve.RserveException
import t.clustering.shared.{Algorithm, WGCNAParams, WGCNAResults}
import t.viewer.shared.ServerError

/**
 * Connects to Rserve to perform a clustering.
 * @param codeDir the root directory where R/InCHlibUtils.R is available.
 */
class RClustering(codeDir: String) {
  val logger = Logger.getLogger("RClustering")

  //Types are Array rather than Seq for easy interop with Java

  private final def safeData(d: Array[Double]) = 
    d.map(d => if (java.lang.Double.isInfinite(d) || java.lang.Double.isNaN(d)) { 0 } else d)
  
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
    r.addCommand(s"data <- c(${safeData(data).mkString(", ")})")
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

  private def simpleStringColumn(xs: Seq[String]): String =
    s"c(${xs.map { "\"" + _ + "\"" }.mkString(", ")})"

  private def simpleColumn(xs: Seq[String]): String =
    s"c(${xs.mkString(", ")})"

  /*
    TODO: check for at least two columns (true minimum: 4?)
   */

  /**
   * Execute a WGCNA clustering.
   * @param params Clustering parameters
   * @param sampleData
   * @param probeNames
   * @param sampleNames
   * @param traitNames IDs of traits. The first trait must be sample_id.
   * @param traitAttributes Values of traits in order. The first trait must be sample_id.
   * @param imageDir Directory to write images
   * @param imageURLBase User-accessible URL base for images written to directory
   * @return
   */
  def doWGCNAClustering(params: WGCNAParams,
                        sampleData: Array[Array[Double]], probeNames: Array[String],
                        sampleNames: Array[String],
                        traitNames: Seq[String],
                        traitAttributes: Seq[Seq[String]],
                        imageDir: String, imageURLBase: String): WGCNAResults = {
    val r = new R
    r.addCommand("library(WGCNA)")
    r.addCommand("options(stringsAsFactors = FALSE)")
    r.addCommand("imageDir <- \"" + imageDir + "\"")
    r.addCommand(s"cutHeight <- ${params.getCutHeight}")
    r.addCommand(s"softPower <- ${params.getSoftPower}")

    r.addCommand("datExpr0 <- data.frame(" +
      sampleData.map(s => "c(" + safeData(s).mkString(", ") + ")").mkString(",") +
      ")")
    r.addCommand(s"names(datExpr0) <- ${simpleStringColumn(probeNames)}")
    r.addCommand(s"rownames(datExpr0) <- ${simpleStringColumn(sampleNames)}")

    r.addCommand("traitData <- data.frame(" +
      simpleStringColumn(traitAttributes.head) + "," +
      traitAttributes.tail.map(simpleColumn).mkString(",") +
      ")")
    r.addCommand(s"names(traitData) <- ${simpleStringColumn(traitNames)}")

    r.addCommand(s"source('$codeDir/R/PanomiconWGCNA.R')")

    try {
      val result = r.execOrThrow()
      println(s"WGCNA result: $result")

      //TODO: take file name directly from R script output
      val sampleClusteringImg = s"$imageURLBase/sampleClustering.png"
      val modulesImg = s"$imageURLBase/modules.png"
      val dendrogramTraitsImg = s"$imageURLBase/dendrogramTraits.png"
      val softThresholdImg = s"$imageURLBase/softThreshold.png"

      val clusters = clustersFromWGCNAModuleFile(s"$imageDir/modules.csv")

      new WGCNAResults(sampleClusteringImg, modulesImg, dendrogramTraitsImg,
        softThresholdImg,
        clusters.map(_._1).toArray, clusters.map(_._2).toArray)
    } catch {
      case e: Exception => throw new ServerError(e.getMessage, e);
    }
  }

  def clustersFromWGCNAModuleFile(path: String): Iterable[(String, Array[String])] = {
    /*
      Example data lines:
      "569","NM_145681","turquoise"
      "570","NM_031769","turquoise"
      "3","NM_053739","yellow"
      "7","NM_024398","yellow"
     */

    val lines = scala.io.Source.fromFile(path).getLines.drop(1).toSeq
    val rawData = lines.map(l => l.split(",").
      map(x => x.replaceAll("\"", "")))

    for {
      (group, lines) <- rawData.groupBy(_(2))
      probes = lines.map(_(1)).toArray
    } yield (group, probes)
  }
}
