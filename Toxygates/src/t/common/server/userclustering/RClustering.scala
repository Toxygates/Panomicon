package t.common.server.userclustering

import otgviewer.server.R
import org.rosuda.REngine.Rserve.RserveException
import t.common.shared.userclustering.Algorithm
import java.util.logging.Logger

class RClustering(userDir: String) {
  val logger = Logger.getLogger("RClustering")

  @throws(classOf[RserveException])
  def clustering(data: Seq[Double], rowName: Seq[String], colName: Seq[String], algorithm: Algorithm = new Algorithm()) = {
    assert(data.length == rowName.length * colName.length)

    val r = new R
    r.addCommand(s"source('$userDir/R/InCHlibUtils.R')")
    r.addCommand(s"data <- c(${data.mkString(", ")})")
    r.addCommand(s"r <- c(${rowName.map { "\"" + _ + "\"" }.mkString(", ")})")
    r.addCommand(s"c <- c(${colName.map { "\"" + _ + "\"" }.mkString(", ")})")
    r.addCommand("rowMethod <- \"" + algorithm.getRowMethod.asParam() + "\"")
    r.addCommand("rowDistance <- \"" + algorithm.getRowDistance.asParam() + "\"")
    r.addCommand("colMethod <- \"" + algorithm.getColMethod.asParam() + "\"")
    r.addCommand("colDistance <- \"" + algorithm.getColDistance.asParam() + "\"")

    r.addCommand("getClusterAsJSON(data, r, c, rowMethod, rowDistance, colMethod, colDistance)")

    r.exec() match {
      case Some(x) => x.asString()
      case None => ""
    }
  }
}
