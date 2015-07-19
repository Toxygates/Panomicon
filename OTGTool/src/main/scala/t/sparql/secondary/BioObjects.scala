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

package t.sparql.secondary
import otg.Species._
import t.db.StoredBioObject

object Protein {
  def unpackB2R(prot: String) = Protein(prot.split("bio2rdf.org/uniprot:")(1))
  def unpackUniprot(protein: String) = Protein(protein.split("uniprot/")(1))
}
/**
 * Identifier is Uniprot accession
 */
case class Protein(identifier: String) extends StoredBioObject[Protein] {
  def packB2R = "http://bio2rdf.org/uniprot:" + identifier
  def packUniprot = "http://purl.uniprot.org/uniprot/" + identifier
}

object Gene {
  def apply(identifier: Int): Gene = apply(identifier.toString)

  def unpackId(geneid: String) = {
    val entrez = geneid.split("geneid:")(1)
    Gene(identifier = entrez, name = entrez)
  }

  def unpackKegg(url: String): Gene = {
    val p1 = url.split(":")(2)
    val p1s = p1.split("_")
    Gene(p1s(1), keggShortCode = p1s(0))
  }
}

/**
 * Identifier is ENTREZ id
 */
case class Gene(identifier: String,
  override val name: String = "",
  symbol: String = "",
  keggShortCode: String = "") extends StoredBioObject[Gene] {

  //e.g. short code: MMU, entrez: 58810 ->
  //<http://bio2rdf.org/kegg:MMU_58810>
  def packKegg = s"http://bio2rdf.org/kegg:${keggShortCode}_$identifier"

  override def equals(other: Any): Boolean = other match {
    case Gene(id, _, _, _) => id == identifier
    case _                 => false
  }
}

object Compound {
  def make(compound: String) = Compound(capitalise(compound)) //preferred builder method
  def capitalise(compound: String) = compound(0).toUpper + compound.drop(1).toLowerCase()
  def unpackChEMBL(url: String) = {
    val ident = url.split("molecule/")(1)
    new Compound(ident, ident)
  }
}

//NB name must be capitalised
case class Compound(override val name: String, identifier: String = "") extends StoredBioObject[Compound] {
  override def hashCode = name.hashCode

  override def equals(other: Any): Boolean = other match {
    case c: Compound => c.name == name
    case _           => false
  }
}

//TODO consider moving out of secondary
case class GOTerm(identifier: String, override val name: String) extends StoredBioObject[GOTerm] {
  override def equals(other: Any): Boolean = other match {
    case GOTerm(id, _) => id == identifier
    case _             => false
  }
}
