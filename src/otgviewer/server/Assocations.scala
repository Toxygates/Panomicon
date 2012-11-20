package otgviewer.server

import scala.collection.JavaConversions._
import otg.OTGOwlim
import otgviewer.shared.DataFilter
import otg.B2RKegg
import java.util.{ Map => JMap, HashMap => JHMap, Set => JSet, HashSet => JHSet, List => JList }
import otgviewer.shared.Association

object Assocations {
  import scala.collection.{Map => CMap, Set => CSet}
  
  //Convert from scala coll types to serialization-safe java coll types.
  def convert(m: CMap[String, CSet[String]]): JHMap[String, JHSet[String]] = {
    val r = new JHMap[String, JHSet[String]]
    addJMultiMap(r, m)    
    r
  }
  
//  def getGoterms(probes: Array[String]): Map[String, Set[String]] = {
//    val rm = Map[String, Set[String]]()
//
//    val sources = List(() => OTGOwlim.mfGoTermsForProbes(probes).map(x => (x._1, x._2.map("MF:" + _))),
//      () => OTGOwlim.bpGoTermsForProbes(probes).map(x => (x._1, x._2.map("BP:" + _))),
//      () => OTGOwlim.ccGoTermsForProbes(probes).map(x => (x._1, x._2.map("CC:" + _))))
//    val rs = sources.par.map(_()).seq
//
//    rs.foreach(addJMultiMap(rm, _))
//    
//    rm
//  }
//  
  def addJMultiMap[K, V](to: JHMap[K, JHSet[V]], from: CMap[K, CSet[V]]) {
    for ((k, v) <- from) {
      if (to.containsKey(k)) {
        to(k).addAll(v)
      } else {
        to.put(k, new JHSet(v))
      }
    }
  }
}