package otgviewer.server

import scala.collection.JavaConversions._
import otg.OTGOwlim
import otgviewer.shared.DataFilter
import otg.B2RKegg
import java.util.{ Map => JMap, HashMap => JHMap, Set => JSet, HashSet => JHSet, List => JList }
import otgviewer.shared.Association

object OwlimServiceImplS {

  def associations(filter: DataFilter, probes: Array[String]): Array[Association] = {

    try {
      B2RKegg.connect()
      OTGOwlim.connect()

      val sources = List(() => ("KEGG pathways", convert(B2RKegg.pathwaysForProbes(probes, Utils.speciesFromFilter(filter)))),
        () => ("GO terms", getGoterms(probes)))

      sources.par.map(_()).seq.map(x => new Association(x._1, x._2)).toArray
    } finally {
      B2RKegg.close()
      OTGOwlim.close()
    }
  }

  //Convert from scala coll types to serialization-safe java coll types.
  def convert(m: Map[String, Set[String]]): JHMap[String, JHSet[String]] = {
    val r = new JHMap[String, JHSet[String]]
    for (k <- m.keys) {
      if (r.containsKey(k)) {
        r(k).addAll(m(k))
      } else {
        r.put(k, new JHSet(m(k)))
      }
    }
    r
  }
  
  def getGoterms(probes: Array[String]): JHMap[String, JHSet[String]] = {
    val rm = new JHMap[String, JHSet[String]]()

    val sources = List(() => OTGOwlim.mfGoTermsForProbes(probes).map(x => (x._1, x._2.map("MF:" + _))),
      () => OTGOwlim.bpGoTermsForProbes(probes).map(x => (x._1, x._2.map("BP:" + _))),
      () => OTGOwlim.ccGoTermsForProbes(probes).map(x => (x._1, x._2.map("CC:" + _))))
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
  }
}