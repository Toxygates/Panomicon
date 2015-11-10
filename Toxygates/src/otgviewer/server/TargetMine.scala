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

package otgviewer.server

import scala.collection.JavaConversions._
import org.intermine.webservice.client.core.ServiceFactory
import org.intermine.webservice.client.services.ListService
import t.sparql.secondary._
import t.sparql._
import otg.sparql._
import t.common.shared.StringList
import otgviewer.server.rpc.Conversions
import otg.sparql.Probes
import t.platform.Probe
import org.intermine.webservice.client.lists.ItemList

object TargetMine {
  import Conversions._
  def getListService(serviceUri: String, user: Option[String] = None,
      pass: Option[String] = None): ListService = {
       println("Connect to TargetMine")
    // TODO this is insecure - ideally, auth tokens should be used.
    val sf = (user, pass) match {
      case (Some(u), Some(p)) => new ServiceFactory(serviceUri, u, p)
      case _                  => new ServiceFactory(serviceUri)
    }

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

  /**
   * Add a set of probe lists by first mapping them to genes
   */
  def addLists(ap: Probes, ls: ListService,
      lists: Iterable[StringList], replace: Boolean): Unit = {

    for (l <- lists) {
      addList(ap, ls, l.items(), Some(l.name()), replace)
    }
  }

  /**
   * Add a probe list by first mapping it into genes
   */
  def addList(probeStore: Probes, ls: ListService,
    input: Seq[String], name: Option[String], replace: Boolean,
    tags: Seq[String] = Seq()): ItemList = {

    var serverList = name.map(ls.getList(_))
    if (serverList != None && replace) {
      ls.deleteList(serverList.get)
    }

    if (serverList == None || replace) {
      val ci = new ls.ListCreationInfo("Gene", name.getOrElse(""))
      val probes = input.map(Probe(_)).toSeq
      //TODO we have the option of doing a fuzzy (e.g. symbol-based) export here
      val genes = probeStore.withAttributes(probes).flatMap(_.genes.map(_.identifier))
      ci.setContent(seqAsJavaList(genes.toList))
      ci.addTags(seqAsJavaList(tags))
      ls.createList(ci)
    } else {
      throw new Exception(s"Unable to add list, $name already existed and replace not requested")
    }
  }

}
