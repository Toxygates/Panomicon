package t.viewer.server

import otg.sparql.Probes

object Platforms {
  def apply(probes: Probes): Platforms = {
    new Platforms(probes.platforms.map(x => x._1 -> x._2.toSet)) 
  }
}

//TODO update mechanism
class Platforms(val data: Map[String, Set[String]]) {

  println("Platforms: ")
  for (p <- data) {
    println(s"\t${p._1}: ${p._2.size}")
  }
  
  /**
   * Filter probes for a number of platforms.
   */
  def filterProbes(probes: Seq[String], platforms: Iterable[String]): Seq[String] = 
    platforms.toSeq.flatMap(pf => filterProbes(probes, pf))

  /**
   * Filter probes for all platforms.
   */
  def filterProbesAllPlatforms(probes: Seq[String]): Seq[String] = {
    val pfs = data.keys
    filterProbes(probes, pfs)    
  }
  
  def platformForProbe(p: String): Option[String] = 
    data.find(_._2.contains(p)).map(_._1)
  
  /**
   * Filter probes for one platform.
   */
  def filterProbes(probes: Seq[String], platform: String): Iterable[String] = {
    if (probes.size == 0) {
      data(platform).toSeq
    } else {
      println(s"Filter ${probes}")
      val r = probes.filter(p => data(platform).contains(p))
      println(s"Result $r")
      r
    }
  }
}