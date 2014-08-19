package otgviewer.server.targetmine

import scala.collection.JavaConversions._
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import otgviewer.server.TargetMine
import t.viewer.shared.StringList
import otgviewer.client.targetmine.TargetmineService
import otg.sparql.Probes
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import t.viewer.server.Configuration
import otg.OTGBConfig
import t.DataConfig
import t.TriplestoreConfig
import t.BaseConfig
import t.viewer.server.Platforms

class TargetmineServiceImpl extends RemoteServiceServlet with TargetmineService {
  var affyProbes: Probes = _ 
  var platforms: Platforms = _
  
    @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config) 
    localInit(Configuration.fromServletConfig(config))    
  }

  // Useful for testing
  def localInit(config: Configuration) {            
    affyProbes = new Probes(config.tsConfig.triplestore)
    platforms = Platforms(affyProbes)
  }
  
  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig =
    OTGBConfig(ts, data)

  override def destroy() {
    affyProbes.close()
    super.destroy()
  }
  
  // TODO: pass in a preferred species, get status info back
  def importTargetmineLists(user: String, pass: String,
    asProbes: Boolean): Array[t.viewer.shared.StringList] = {
    val ls = TargetMine.getListService(user, pass)    
    val tmLists = ls.getAccessibleLists()
    tmLists.filter(_.getType == "Gene").map(
      l => {
        val tglist = TargetMine.asTGList(l, affyProbes, platforms.filterProbesAllPlatforms(_))
        if (tglist.items.size > 0) {
          val probesForCurrent = platforms.filterProbes(tglist.items, List())
          tglist.setComment(probesForCurrent.size + "");
        } else {
          tglist.setComment("0")
        }
        tglist
      }).toArray
  }

  def exportTargetmineLists(user: String, pass: String, 
      lists: Array[StringList], replace: Boolean): Unit = {
    val ls = TargetMine.getListService(user, pass)
    TargetMine.addLists(affyProbes, ls, lists.toList, replace)
  }    
}