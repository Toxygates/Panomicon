/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.viewer.server.intermine

import scala.collection.JavaConversions._

import org.intermine.webservice.client.results.TabTableResult

import t.viewer.shared.StringList
import t.sparql.Probes
import t.viewer.client.intermine.IntermineService
import t.viewer.server.Configuration
import t.viewer.server.Platforms
import t.viewer.server.rpc.TServiceServlet
import t.viewer.shared.intermine._

abstract class IntermineServiceImpl extends TServiceServlet with IntermineService {
  var affyProbes: Probes = _
  var platforms: Platforms = _
  var mines: Intermines = _

  // Useful for testing
  override def localInit(config: Configuration) {
    super.localInit(config)
    affyProbes = context.probes
    platforms = Platforms(affyProbes)
    mines = new Intermines(config.intermineInstances)
  }

  override def destroy() {
    affyProbes.close()
    super.destroy()
  }

  // TODO: pass in a preferred species, get status info back
  def importLists(inst: IntermineInstance, user: String, pass: String,
    asProbes: Boolean): Array[StringList] = {
    val conn = mines.connector(inst, platforms)
    try {
      val ls = conn.getListService(Some(user), Some(pass))
      val imLists = ls.getAccessibleLists()

      println("Accessible lists: ")

      for (iml <- imLists) {
        println(s"${iml.getName} ${iml.getType} ${iml.getSize}")
      }

      val tglists = for (iml <- imLists;
        if iml.getType == "Gene";
        tglist = conn.asTGList(iml, affyProbes, platforms.filterProbesAllPlatforms)
        ) yield tglist

      for (l <- tglists.flatten) {
        if (l.items.size > 0) {
//          val probesForCurrent = platforms.filterProbes(l.items, List())
//          l.setComment(probesForCurrent.size + "");
          l.setComment(l.items.size + "")
        } else {
          l.setComment("0")
        }
      }
      tglists.flatten.toArray

    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new IntermineException(e.getMessage)
    }
  }

  def exportLists(inst: IntermineInstance, user: String, pass: String,
      lists: Array[StringList], replace: Boolean): Unit = {
    val conn = mines.connector(inst, platforms)
    try {
      val ls = conn.getListService(Some(user), Some(pass))
      conn.addProbeLists(ls, lists.toList, replace)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new IntermineException(e.getMessage)
    }
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
//    val tags = List("H. sapiens") //!!

    val tempList = conn.addProbeList(ls, list.items(),
      None, false, tags)

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
        headers +: res.getIterator.toArray.map(adjustEnrichResult(_).toArray)

      case None => throw new IntermineException("Unable to create temporary list for enrichment")
    }

  }

  def getSession(inst: IntermineInstance): String = {
    val conn = mines.connector(inst, platforms)
    conn.getSessionToken()
  }
}
