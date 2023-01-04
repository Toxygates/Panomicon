/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.server.viewer.intermine

import org.intermine.client.results.TabTableResult
import t.sparql.ProbeStore
import t.gwt.viewer.client.intermine.IntermineService
import t.platform.PlatformRegistry
import t.server.viewer.Configuration
import t.server.viewer.rpc.TServiceServlet
import t.shared.viewer.StringList
import t.shared.viewer.intermine._

import scala.collection.JavaConverters._

class IntermineServiceImpl extends TServiceServlet with IntermineService {
  var affyProbes: ProbeStore = _
  var mines: Intermines = _
  lazy val platforms = context.platformRegistry

  // Useful for testing
  override def localInit(config: Configuration) {
    super.localInit(config)
    affyProbes = context.probeStore
    mines = new Intermines(config.intermineInstances)
  }

  override def destroy() {
    affyProbes.close()
    super.destroy()
  }

  // Task: pass in a preferred species, get status info back
  def importLists(inst: IntermineInstance, user: String, pass: String): Array[StringList] = {
    val conn = mines.connector(inst, platforms)
    conn.importLists(user, pass).map(l =>
      new StringList(StringList.PROBES_LIST_TYPE, l._2, l._1)
    ).toArray
  }

  def exportLists(inst: IntermineInstance, user: String, pass: String,
      lists: Array[StringList], replace: Boolean): Unit = {
    val conn = mines.connector(inst, platforms)
    conn.exportLists(user, pass,
      lists.map(l => (l.items().toSeq, l.name())), replace)
  }

  // This is mainly to adjust the formatting of the p-value
  private def adjustEnrichResult(res: Seq[String]): Seq[String] = {
    Seq(res(0), res(1),
        "%.3g".format(res(2).toDouble),
        res(3))
  }

  def multiEnrichment(inst: IntermineInstance, lists: Array[StringList],
      params: EnrichmentParams, session: String): Array[Array[Array[String]]] =
    lists.map(enrichment(inst, _, params, session)).toArray

  def enrichment(inst: IntermineInstance, list: StringList,
                 params: EnrichmentParams, session: String): Array[Array[String]] = {
    println(s"Enrichment in session $session")

    val conn = mines.connector(inst, platforms)
    val ls = conn.getListService(None, None)
    ls.setAuthentication(session)
    val tags = List()

    val tempList = conn.addProbeList(ls, list.items(), "temp_enrichment", false, tags)

    tempList match {
      case Some(tl) =>
        val listName = tl.getName
        println(s"Created temporary list $listName")

        val request = conn.enrichmentRequest(ls)
        request.setAuthToken(session)
        //      request.addParameter("token", apiKey)
        request.addParameter("list", listName)
        request.addParameter("widget", params.widget.getKey)
        request.addParameter("maxp", params.cutoff.toString())
        request.addParameter("correction", params.correction.getKey())
        request.addParameter("filter", params.filter)

        val con = ls.executeRequest(request)
        println("Response code: " + con.getResponseCode)
        val res = new TabTableResult(con)
        ls.deleteList(tl)
        val headers = Array("ID", "Description", "p-value", "Matches")
        headers +: res.getIterator.asScala.toArray.map(r => adjustEnrichResult(r.asScala).toArray)

      case None => throw new IntermineException("Unable to create temporary list for enrichment")
    }

  }

  def getSession(inst: IntermineInstance): String = {
    val conn = mines.connector(inst, platforms)
    conn.getSessionToken()
  }
}
