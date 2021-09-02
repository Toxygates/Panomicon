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

import scala.Vector
import scala.collection.JavaConverters._
import org.intermine.webservice.client.core.ContentType
import org.intermine.webservice.client.core.ServiceFactory
import org.intermine.webservice.client.lists.ItemList
import org.intermine.webservice.client.services.ListService
import org.json.JSONObject
import t.shared.viewer.StringList
import t.sparql._
import t.sparql.ProbeStore
import t.sparql.secondary._
import t.shared.viewer.intermine._
import t.intermine._
import t.platform.PlatformRegistry

class IntermineConnector(instance: IntermineInstance,
    platforms: PlatformRegistry) extends t.intermine.Connector(instance.appName,
      instance.serviceURL) {

  def title = instance.title()

  def getListService(user: Option[String] = None,
    pass: Option[String] = None): ListService = {
    println(s"Connect to $title")
    // Task: this is insecure; auth tokens should be used.
    val sf = (user, pass) match {
      case (Some(u), Some(p)) => InsecureServiceFactory.fromUserAndPass(serviceUrl, u, p)
      case _                  => new ServiceFactory(serviceUrl)
    }

    sf.setApplicationName(appName)
    sf.getListService()
  }

  def enrichmentRequest(ls: ListService) =
    ls.createGetRequest(serviceUrl + "/list/enrichment", ContentType.TEXT_TAB)

  def asTGList(l: org.intermine.webservice.client.lists.ItemList,
               ap: ProbeStore,
               filterProbes: (Seq[String]) => Seq[String]): Option[StringList] = {
    var items: Vector[Gene] = Vector()
    println(s"Importing ${l.getName}")

    for (i <- 0 until l.getSize()) {
      val it = l.get(i)
      items :+= Gene(it.getString("Gene.primaryIdentifier"))
    }

    //we will have obtained the genes as ENTREZ identifiers
    println(s"${items take 100} ...")
    var probes = items.flatMap(g => platforms.geneLookup.get(g)).
      flatten.map(_.identifier).distinct

    val filtered = if (!probes.isEmpty) {
      filterProbes(probes)
    } else {
      println(s"Warning: the following imported list had no corresponding probes in the system: ${l.getName}")
      println(s"The original size was ${l.getSize}")
      return None
    }
    println(s"${filtered take 100} ...")

    Some(new StringList(StringList.PROBES_LIST_TYPE,
        l.getName(), filtered.toArray))
  }

  /**
   * Add a set of probe lists by first mapping them to genes
   */
  def addProbeLists(ls: ListService,
    lists: Iterable[StringList], replace: Boolean): Unit = {

    for (l <- lists) {
      addProbeList(ls, l.items(), Some(l.name()), replace)
    }
  }

  /**
   * Obtain a valid list name, or None if the export cannot proceed.
   * This potentially deletes a pre-existing list.
   */
  private def validNameForExport(ls: ListService, name: Option[String],
      replace: Boolean): Option[String] = {

    var serverList = name.map(n => Option(ls.getList(n))).flatten
    if (serverList != None && replace) {
      ls.deleteList(serverList.get)
    }

    //the Set: prefix gets appended by the front-end
    if (serverList == None && name != None) {
      val altName = name.get.split("Set:")
      if (altName.size > 1) {
        serverList = Option(ls.getList(altName(1)))
        if (serverList != None) {
          println(s"Assume list ${name.get} corresponds to ${altName(1)} on server")
        }
      }
    }
    if (serverList != None && replace) {
      println(s"Delete list $serverList for replacement")
      ls.deleteList(serverList.get)
    }

    val useName = name.getOrElse("")
    if (serverList == None || replace) {
      Some(useName)
    } else {
      val n = name.getOrElse("")
      //Could report this message to the user somehow
      println(s"Not exporting list '$n' since it already existed (replacement not requested)")
      //      throw new IntermineException(
      //        s"Unable to add list, ${name.get} already existed and replacement not requested")
      None
    }
  }

  /**
   * Add a probe list to InterMine by first mapping it into genes (lazily)
   */
  def addProbeList(ls: ListService,
    probes: Iterable[String], name: Option[String], replace: Boolean,
    tags: Seq[String] = Seq("toxygates")): Option[ItemList] = {
    addEntrezList(ls,
        () =>
          platforms.resolve(probes.toSeq).flatMap(_.genes.map(_.identifier)),
          name, replace, tags)
  }
    /**
   *  Add a list of NCBI/Entrez genes to InterMine
   */
  def addEntrezList(ls: ListService, getGenes: () => Iterable[String],
    name: Option[String], replace: Boolean,
    tags: Seq[String] = Seq("toxygates")): Option[ItemList] = {
    validNameForExport(ls, name, replace) match {
      case Some(useName) =>
        val ci = new ls.ListCreationInfo("Gene", useName)
        //Note: we have the option of doing a fuzzy (e.g. symbol-based) export here
        ci.setContent(getGenes().toSeq.asJava)
        ci.addTags(tags.asJava)
        println(s"Exporting list '$useName'")
        Some(ls.createList(ci))
      case None =>
        None
    }
  }
}

class Intermines(instances: Iterable[IntermineInstance]) {
  def connector(inst: IntermineInstance, platforms: PlatformRegistry) = {
    //Simple security/sanity check - refuse to connect to a URL that
    //we didn't know about from before. This is because instance objects
    //may be passed from the client side.

    if(!allURLs.contains(inst.serviceURL())) {
      throw new Exception("Invalid instance")
    }
    new IntermineConnector(inst, platforms)
  }

  lazy val allURLs = instances.map(_.serviceURL()).toSeq.distinct
  lazy val all = instances.toSeq.distinct
//
  lazy val byTitle = Map() ++ all.map(m => m.title -> m)
}
