package t.platform.affy

sealed trait AffyColumn

/**
 * Abstract base class for affymetrix columns.
 * Columns are identified by their title in the input data, so this must be correct
 * (case-sensitive)
 */
abstract class AAffyColumn(val title: String, val annotKey: Option[String],
    val isList: Boolean) extends AffyColumn {

  //TODO support e.g. quoted strings so we don't need to remove commas
  private def escape(x: String) = x.replace(",", " -")

  def annotations(data: String): Option[String] = {
    if (isList) {
      val raw = procListItem(data).filter(!_.isEmpty)
      if (!raw.isEmpty) {
        Some(raw.map(s => s"${annotKey.get}=${escape(s(0))}").mkString(","))
      } else {
        None
      }
    } else {
      if (data.trim != "---") {
        Some(s"${annotKey.get}=${escape(data)}")
      } else {
        None
      }
    }
  }

  def procListItem(x: String): Seq[Seq[String]] = {
    val records = x.split("///").toVector
    records.map(_.split("//").map(_.trim).filter(_ != "---").toVector)
  }

  val key = annotKey.get
}

object GOBP extends AAffyColumn("Gene Ontology Biological Process", Some("gobp"), true)
object GOCC extends AAffyColumn("Gene Ontology Cellular Component", Some("gocc"), true)
object GOMF extends AAffyColumn("Gene Ontology Molecular Function", Some("gomf"), true)
object Swissprot extends AAffyColumn("SwissProt", Some("swissprot"), true)
object RefseqTranscript extends AAffyColumn("RefSeq Transcript ID", Some("refseqTrn"), true)
object RefseqProtein extends AAffyColumn("RefSeq Protein ID", Some("refseqPro"), true)
object ProbeID extends AAffyColumn("Probe Set ID", None, false)
object GeneChip extends AAffyColumn("GeneChip Array", Some("genechip"), false)
object Title extends AAffyColumn("Gene Title", Some("title"), false)
object Symbol extends AAffyColumn("Gene Symbol", Some("symbol"), true)
object Entrez extends AAffyColumn("Entrez Gene", Some("entrez"), true)
object Species extends AAffyColumn("Species Scientific Name", Some("species"), false)
object Unigene extends AAffyColumn("UniGene ID", Some("unigene"), false)
object Ensembl extends AAffyColumn("Ensembl", Some("ensembl"), true)
object EC extends AAffyColumn("EC", Some("EC"), true)
