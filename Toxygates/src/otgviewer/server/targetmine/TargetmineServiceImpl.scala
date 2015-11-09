/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.server.targetmine

import scala.collection.JavaConversions._
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import otgviewer.server.TargetMine
import t.common.shared.StringList
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
import otgviewer.server.rpc.OTGServiceServlet
import org.intermine.webservice.client.services.ListService
import java.util.Arrays
import org.intermine.webservice.client.core.ContentType
import org.intermine.webservice.client.results.JSONResult
import org.intermine.webservice.client.results.TabTableResult

class TargetmineServiceImpl extends OTGServiceServlet with TargetmineService {
  var affyProbes: Probes = _
  var platforms: Platforms = _
  //TODO how to best initialise this?
  val serviceUri = "http://targetmine.mizuguchilab.org/targetmine/service"

  // Useful for testing
  override def localInit(config: Configuration) {
    super.localInit(config)
    affyProbes = context.probes
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
    asProbes: Boolean): Array[t.common.shared.StringList] = {
    val ls = TargetMine.getListService(serviceUri, user, pass)
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
    val ls = TargetMine.getListService(serviceUri, user, pass)
    TargetMine.addLists(affyProbes, ls, lists.toList, replace)
  }

  def enrichment(user: String, pass: String, list: StringList): Unit = {
      val ls = TargetMine.getListService(serviceUri, user, pass)
      val tags = List("H. sapiens") //!!

      val tempList = TargetMine.addList(affyProbes, ls, list.items(),
          None, false, tags)

      val listName = tempList.getName
      println(s"Created temporary list $listName")

      val widget = "gene_pathway_enrichment"
      val maxp = 0.05
      val corrMethod = "Benjamini Hochberg"
      val filter = "All"

      val request = ls.createGetRequest(serviceUri + "/list/enrichment", ContentType.TEXT_TAB)
      request.addParameter("list", listName)
      request.addParameter("widget", widget)
      request.addParameter("maxp", maxp.toString)
      request.addParameter("correction", corrMethod)
      request.addParameter("filter", filter)

      val con = ls.executeRequest(request)
      println("Response code: " + con.getResponseCode)
      val res = new TabTableResult(con)
      for (r <- res.getIterator) {
        println(r.mkString("\t"))
      }

      ls.deleteList(tempList)
  }
}
