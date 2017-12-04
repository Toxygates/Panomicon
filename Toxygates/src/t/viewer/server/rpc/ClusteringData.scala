package t.viewer.server.rpc

import scala.collection.JavaConversions._

import org.apache.commons.lang.StringUtils

import t.common.shared.ValueType
import t.platform.Probe
import t.sparql.Probes
import t.viewer.server.MatrixController

class ClusteringData(controller: MatrixController,
                     probeStore: Probes,
                     rows: Seq[String],
                     valueType: ValueType) extends t.clustering.server.ClusteringData {

  val mm = controller.managedMatrix
  val mat = if (rows != null && rows.length > 0) {
    mm.current.selectRowsFromAtomics(rows)
  } else {
    mm.current
  }

  val info = mm.info

  val allRows = mat.asRows

  val columns = mat.sortedColumnMap.filter(x => !info.isPValueColumn(x._2))

  private def joinedAbbreviated(items: Iterable[String], n: Int): String =
    StringUtils.abbreviate(items.toSeq.distinct.mkString("/"), 30)

  def rowNames: Array[String] =
    allRows.map(r => joinedAbbreviated(r.getAtomicProbes, 20)).toArray

  def colNames: Array[String] = columns.map(_._1).toArray

  def codeDir: String = ???

  /**
   * Obtain column-major data for the specified rows and columns
   */
  def data: Array[Array[Double]] = mat.selectColumns(columns.map(_._2)).data.
    map(_.map(_.value).toArray).toArray

  /**
   * Gene symbols for the specified rows
   */
  def geneSymbols: Array[String] = {
    val allAtomics = allRows.flatMap(_.getAtomicProbes.map(p => Probe(p)))
    val aaLookup = Map() ++ probeStore.withAttributes(allAtomics).map(a => a.identifier -> a)

    allRows.map(r => {
      val atrs = r.getAtomicProbes.map(aaLookup(_))
      joinedAbbreviated(atrs.flatMap(_.symbols.map(_.symbol)), 20)
    }).toArray
  }
}
