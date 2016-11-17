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

package t.platform.affy

sealed trait AffyColumn

/**
 * Abstract base class for affymetrix columns.
 * Columns are identified by their title in the input data, so this must be correct
 * (case-sensitive)
 */
abstract class AAffyColumn(val title: String, val annotKey: Option[String],
    val isList: Boolean = true) extends AffyColumn {

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

  lazy val key = annotKey.get
}

object GOBP extends AAffyColumn("Gene Ontology Biological Process", Some("gobp"))
object GOCC extends AAffyColumn("Gene Ontology Cellular Component", Some("gocc"))
object GOMF extends AAffyColumn("Gene Ontology Molecular Function", Some("gomf"))
object Swissprot extends AAffyColumn("SwissProt", Some("swissprot"))
object RefseqTranscript extends AAffyColumn("RefSeq Transcript ID", Some("refseqTrn"))
object RefseqProtein extends AAffyColumn("RefSeq Protein ID", Some("refseqPro"))
object ProbeID extends AAffyColumn("Probe Set ID", None, false)
object GeneChip extends AAffyColumn("GeneChip Array", Some("genechip"), false)
object Title extends AAffyColumn("Gene Title", Some("title"))
object Symbol extends AAffyColumn("Gene Symbol", Some("symbol"))
object Entrez extends AAffyColumn("Entrez Gene", Some("entrez"))
object Species extends AAffyColumn("Species Scientific Name", Some("species"), false)
object Unigene extends AAffyColumn("UniGene ID", Some("unigene"))
object Ensembl extends AAffyColumn("Ensembl", Some("ensembl"))
object EC extends AAffyColumn("EC", Some("EC"))
