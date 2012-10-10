package otgviewer.server

import scala.collection.JavaConversions._
import otg.OTGOwlim
import otgviewer.shared.DataFilter
import otg.B2RKegg

import java.util.{Map => JMap, HashMap => JHMap, Set => JSet, HashSet => JHSet}

object OwlimServiceImplS {

  def associations(filter: DataFilter, probes: Array[String]): JHMap[String, JHSet[String]] = {
        
    try {
      B2RKegg.connect()
      OTGOwlim.connect()
      
      val rm = new JHMap[String, JHSet[String]]
      
      val sources = List(() => B2RKegg.pathwaysForProbes(probes, Utils.speciesFromFilter(filter)),
          () => OTGOwlim.goTermsForProbes(probes))
      val rs = sources.par.map(_()).seq
      for (r <- rs; (k, v) <- r) {
//        println(k + " -> " + v)
        if (rm.containsKey(k)) {
          rm.get(k).addAll(v)
        } else {
          rm.put(k, new JHSet(v))
        }
      }
      rm      
    } finally {
      B2RKegg.close()
      OTGOwlim.close()
    }
  } 
}