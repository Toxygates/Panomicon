package t.platform

import t.db.BioObject
import t.sparql.secondary.Gene
import t.sparql.secondary.Protein
import t.sparql.Probes

object Probe {
  import Probes._
  private val split = defaultPrefix + "/" 
  def unpack(uri: String) = Probe(uri.split(split)(1))
}

//TODO reconsider which members should be standard for a T application in general
//Most of these are used by Toxygates, but not by Tritigate
case class Probe(val identifier: String, override val name: String = "",
    val titles: Iterable[String] = Set(),
    val proteins: Iterable[Protein] = Set(), 
    val genes: Iterable[Gene] = Set(),
    val symbols: Iterable[Gene] = Set(),
    val platform: String = "") extends BioObject[Probe] {
  
  def symbolStrings = symbols.map(_.symbol)
  
  //GenBioObject overrides hashCode
  override def equals(other: Any): Boolean = other match {
      case Probe(id, _, _, _, _, _, _) => id == identifier
      case _ => false    
  }
  
  def pack = Probes.defaultPrefix + "/" + identifier
}

//TODO consider retiring SimpleProbe
case class SimpleProbe(id: String, platformId: String = "") 

case class OrthologGroup(title: String, probes: Iterable[SimpleProbe]) {
  def id = title // TODO reconsider
  def subIds: Iterable[String] = probes.map(_.id)
  def platforms: Iterable[String] = probes.map(_.platformId).toSet
}