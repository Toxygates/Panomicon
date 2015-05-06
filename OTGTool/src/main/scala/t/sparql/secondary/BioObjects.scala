package t.sparql.secondary
import otg.Species._
import t.db.BioObject

object Protein {
  def unpackB2R(prot: String) = Protein(prot.split("bio2rdf.org/uniprot:")(1))
  def unpackUniprot(protein: String) = Protein(protein.split("uniprot/")(1))
}
/**
 * Identifier is Uniprot accession
 */
case class Protein(identifier: String) extends BioObject[Protein] {
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
                keggShortCode: String = "") extends BioObject[Gene] {

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
case class Compound(override val name: String, identifier: String = "") extends BioObject[Compound] {
  override def hashCode = name.hashCode

  override def equals(other: Any): Boolean = other match {
    case c: Compound => c.name == name
    case _           => false
  }
}

//TODO consider moving out of secondary
case class GOTerm(identifier: String, override val name: String) extends BioObject[GOTerm] {
  override def equals(other: Any): Boolean = other match {
    case GOTerm(id, _) => id == identifier
    case _             => false
  }  
}