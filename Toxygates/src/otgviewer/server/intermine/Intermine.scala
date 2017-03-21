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

package otgviewer.server.intermine

import scala.collection.JavaConversions._

import org.intermine.webservice.client.core.ContentType
import org.intermine.webservice.client.core.ServiceFactory
import org.intermine.webservice.client.lists.ItemList
import org.intermine.webservice.client.services.ListService
import t.sparql.secondary._
import t.sparql._
import otg.sparql._
import otg.sparql.Probes
import otgviewer.shared.intermine._
import t.common.shared.StringList
import otgviewer.server.rpc.Conversions
import t.platform.Probe
import scala.Vector

class IntermineConnector(instance: IntermineInstance) {
  import Conversions._

  def title = instance.title()
  def appName = instance.appName()
  def serviceUrl = instance.serviceURL

  def getListService(user: Option[String] = None,
    pass: Option[String] = None): ListService = {
    println(s"Connect to $title")
    // TODO this is insecure - ideally, auth tokens should be used.
    val sf = (user, pass) match {
      case (Some(u), Some(p)) => new ServiceFactory(serviceUrl, u, p)
      case _                  => new ServiceFactory(serviceUrl)
    }

    sf.setApplicationName(appName)
    sf.getListService()
  }

  def enrichmentRequest(ls: ListService) =
    ls.createGetRequest(serviceUrl + "/list/enrichment", ContentType.TEXT_TAB)

  def asTGList(l: org.intermine.webservice.client.lists.ItemList,
    ap: Probes,
    filterProbes: (Seq[String]) => Seq[String]): Option[StringList] = {
    var items: Vector[Gene] = Vector()    
    println(s"Importing ${l.getName}")
    
    if (l.getSize > 1000) {
      println("List too big - not importing")
      return None
    }
    
    for (i <- 0 until l.getSize()) {
      val it = l.get(i)
      items :+= Gene(it.getString("Gene.primaryIdentifier"))
    }
    //we will have obtained the genes as ENTREZ identifiers
    println(items)
    val probes = ap.forGenes(items).map(_.identifier).toSeq
    println(probes)
    val filtered = if (!probes.isEmpty) {
      filterProbes(probes) 
    } else {
      println(s"Warning: the following imported list had no corresponding probes in the system: ${l.getName}")
      println(s"The original size was ${l.getSize}")
      Seq()
    }
    println(filtered)

    Some(new StringList(StringList.PROBES_LIST_TYPE, 
        l.getName(), filtered.toArray))
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
    tags: Seq[String] = Seq()): Option[ItemList] = {

    var serverList = name.map(n => Option(ls.getList(n))).flatten
    if (serverList != None && replace) {      
      ls.deleteList(serverList.get)
    }
    
    //the Set: prefix gets appended by the GUI
    if (serverList == None && name != None) {
      val altName = name.get.split("Set:")
      if (altName.size > 1) {
        serverList = Option(ls.getList(altName(1)))        
      }
    }
    
    if (serverList != None && replace) {
      println(s"Delete list $serverList for replacement")
      ls.deleteList(serverList.get)
    }

    if (serverList == None || replace) {
      val ci = new ls.ListCreationInfo("Gene", name.getOrElse(""))
      val probes = input.map(Probe(_)).toSeq
      //TODO we have the option of doing a fuzzy (e.g. symbol-based) export here
      val genes = probeStore.withAttributes(probes).flatMap(_.genes.map(_.identifier))
      ci.setContent(seqAsJavaList(genes.toList))
      ci.addTags(seqAsJavaList(tags))
      Some(ls.createList(ci))
    } else {
      //Could report this message to the user somehow
      println(s"Not exporting list ${name.get} since it already existed (replacement not requested)")
//      throw new IntermineException(
//        s"Unable to add list, ${name.get} already existed and replacement not requested")
      None
    }
  }
}

class Intermines(instances: Iterable[IntermineInstance]) {
  def connector(inst: IntermineInstance) = {
    //Simple security/sanity check - refuse to connect to a URL that
    //we didn't know about from before. This is because instance objects
    //may be passed from the client side.

    if(!allURLs.contains(inst.serviceURL())) {
      throw new Exception("Invalid instance")
    }
    new IntermineConnector(inst)
  }

  lazy val allURLs = instances.map(_.serviceURL()).toSet
  lazy val all = instances.toSet
//
//  lazy val byTitle = Map() ++ all.map(m => m.title -> m)
}
