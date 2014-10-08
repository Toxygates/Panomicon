package t.common.shared.probe

import t.platform.OrthologMapping

/**
 * A ProbeMapper maps probes from one domain to another.
 * Examples: transcripts to genes, genes to proteins, etc.
 * In the general case, the mapping is many-to-many.
 */
trait ProbeMapper {

  def toRange(p: String): Iterable[String] 
  def toDomain(p: String): Iterable[String]
}

class OrthologProbeMapper(mapping: OrthologMapping) extends ProbeMapper {
  
  def toRange(p: String) = mapping.forProbe(p)
  val reverse = Map() ++ mapping.forProbe.toSeq.flatMap(x => (x._2.map(y => (y, x._1)))).
  	groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
  def toDomain(p: String) = reverse(p)	  
}