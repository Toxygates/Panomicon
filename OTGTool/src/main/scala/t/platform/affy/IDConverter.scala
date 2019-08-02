package t.platform.affy

/**
 * An ID conversion table that converts non-affy gene and probe IDs,
 * e.g. ensembl, into affy IDs, based on a set of affymetrix annotations.
 */
class IDConverter(affyFile: String, column: AffyColumn) {

  val data = Converter.loadColumns(affyFile, Array(ProbeID, column))

  val foreignToAffy: Map[String, Seq[String]] = {
    val raw = (for {
      Seq(affy, foreigns) <- data
      foreign <- column.expandList(foreigns)
      pair = (affy, foreign)
    } yield pair)

    raw.groupBy(_._2).map { case (foreign, data) => (foreign -> data.map(_._1)) }
  }

  println("ID conversion sample entries: ")
  for (d <- foreignToAffy take 10) println(d)

}
