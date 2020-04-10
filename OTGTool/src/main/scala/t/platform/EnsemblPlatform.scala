package t.platform

import friedrich.util.formats.TSVFile
import t.db.ProbeId

object EnsemblPlatform {

  //Convert e.g. XM_931290.1 into XM_931290
  def removeVersionNumber(probe: String) = {
    probe.split("\\.")(0)
  }

  val probeIdColumn = "probe_id"
  val refseqColumn = "refseq_id"

  /**
   * Expects a conversion file (TSV) where one column defines the probe ID in some foreign format,
   * and the other defines the ensembl RefSeq ID.
   * The former is mapped into the latter.
   * @param file
   */
  def loadConversionTable(file: String): Map[ProbeId, Iterable[ProbeId]] = {
    val colMap = TSVFile.readMap("", file, true)
    val processedRS = colMap(refseqColumn).map(removeVersionNumber)
    val mapping = colMap(probeIdColumn) zip processedRS
    mapping.groupBy(_._1).map(x => (x._1, x._2.map(_._2))).filter(_._2.nonEmpty)
  }

}
