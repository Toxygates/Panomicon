package t.platform.affy

/**
 * An ID conversion table that converts non-affy gene and probe IDs,
 * e.g. ensembl, into affy IDs, based on a set of affymetrix annotations.
 */
class IDConverter(affyFile: String, column: AffyColumn) {

  val data = Converter.loadColumns(affyFile, Seq(ProbeID, column))

  val foreignToAffy: Map[String, Seq[String]] = data.groupBy(x => x(1)).
    map { case (specialCol, data) => (specialCol -> data.map(_(0))) }
}
