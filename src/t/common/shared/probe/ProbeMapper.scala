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
  
  def domain: Iterable[String]
  
  /**
   * A consistent ordering must be provided
   */
  def range: Seq[String]
}

class OrthologProbeMapper(mapping: OrthologMapping) extends ProbeMapper {
  var tmp = mapping.forProbe.toSeq.map(x => (x._2, x._2.mkString("/")))
  val forward = Map() ++ tmp.flatMap(m => m._1.map(x => x -> m._2))
  val reverse = Map() ++ tmp.map(m => m._2 -> m._1)
  
  def toRange(p: String) = List(forward(p))  
  def toDomain(p: String) = reverse(p)	  
  
  def domain = forward.keySet
  val range = reverse.keySet.toSeq
  println("Range: " + range.size + " " + range.take(50))
}