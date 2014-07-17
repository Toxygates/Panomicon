package otgviewer.server

import scala.collection.JavaConversions._
import otgviewer.shared.DataFilter
import java.util.{ Map => JMap, HashMap => JHMap, Set => JSet, HashSet => JHSet, List => JList }
import otgviewer.shared.Association
import t.common.shared.Pair
import otgviewer.server.rpc.Conversions

object Assocations {
  import scala.collection.{Map => CMap, Set => CSet}
  import Conversions._
  
  //Convert from scala coll types to serialization-safe java coll types.
  def convertPairs(m: CMap[String, CSet[(String, String)]]): JHMap[String, JHSet[Pair[String, String]]] = {
    val r = new JHMap[String, JHSet[Pair[String, String]]]    
    val mm: CMap[String, CSet[Pair[String, String]]] = m.map(k => (k._1 -> k._2.map(asJava(_))))
    addJMultiMap(r, mm)  
    r
  }
  
   def convert(m: CMap[String, CSet[String]]): JHMap[String, JHSet[String]] = {
    val r = new JHMap[String, JHSet[String]]
    addJMultiMap(r, m)  
    r
  }
  
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