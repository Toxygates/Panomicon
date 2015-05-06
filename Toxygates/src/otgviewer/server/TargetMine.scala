package otgviewer.server

import scala.collection.JavaConversions._
import org.intermine.webservice.client.core.ServiceFactory
import org.intermine.webservice.client.services.ListService
import t.sparql.secondary._
import t.sparql._
import otg.sparql._
import t.viewer.shared.StringList
import otgviewer.server.rpc.Conversions
import otg.sparql.Probes
import t.platform.Probe

object TargetMine {
  import Conversions._
  def getListService(user: String, pass: String): ListService = {
       println("Connect to TargetMine")
    // TODO this is insecure - ideally, auth tokens should be used.
    val sf = new ServiceFactory("http://targetmine.nibio.go.jp/targetmine/service", user, pass)
    sf.setApplicationName("targetmine")
    sf.getListService()
  }
    
  def asTGList(l: org.intermine.webservice.client.lists.ItemList,
      ap: Probes,
      filterProbes: (Seq[String]) => Seq[String]): StringList = {
      var items: Vector[Gene] = Vector()
      for (i <- 0 until l.getSize()) {        
        val it = l.get(i)
        items :+= Gene(it.getString("Gene.primaryIdentifier"))        
      }
      //we will have obtained the genes as ENTREZ identifiers
      println(items)      
      val probes = ap.forGenes(items).map(_.identifier).toSeq
      println(probes)    
      val filtered = filterProbes(probes)
      println(filtered)
    
      new StringList("probes", l.getName(), filtered.toArray);
  }
  
  def addLists(ap: Probes, ls: ListService, 
      lists: List[StringList], replace: Boolean): Unit = {
    for (l <- lists) {
      var serverList = ls.getList(l.name)
      if (serverList != null && replace) {
        ls.deleteList(serverList)
      }
      if (serverList == null || replace) {
        val ci = new ls.ListCreationInfo("Gene")
        val probes = l.items.map(Probe(_)).toSeq
        //TODO we have the option of doing a fuzzy (e.g. symbol-based) export here
        val genes = ap.withAttributes(probes).flatMap(_.genes.map(_.identifier))
        ci.setContent(asJavaList(genes.toList))
        var newList = ls.createList(ci)
        newList = ls.rename(newList, l.name)
      } 
    }
  }
}