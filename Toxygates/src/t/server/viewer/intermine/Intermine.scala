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

import scala.collection.JavaConverters._
import org.intermine.client.core.ContentType
import org.intermine.client.core.ServiceFactory
import org.intermine.client.lists.ItemList
import org.intermine.client.services.ListService
import t.sparql.secondary._
import t.shared.viewer.intermine._
import t.intermine._
import t.platform.PlatformRegistry

/** A named list of probes or genes */
case class GeneList(name: String, items: Seq[String])

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

  /** Convert an intermine ItemList into a GeneList,
   * preserving only those items that are known to the platforms in our system. */
  def asTGList(l: ItemList): Option[GeneList] = {
    var items: Vector[Gene] = Vector()
    println(s"Importing ${l.getName}")

    for (i <- 0 until l.getSize()) {
      val it = l.get(i)
      items :+= Gene(it.getString("Gene.primaryIdentifier"))
    }

    //we will have obtained the genes as ENTREZ identifiers
    println(s"${items take 100} ...")
    val probes = items.flatMap(g => platforms.geneLookup.get(g)).
      flatten.map(_.identifier).distinct

    val filtered = platforms.filterProbesAllPlatforms(probes)

    if (filtered.isEmpty) {
      println(s"Warning: the following imported list had no corresponding probes in the system: ${l.getName}")
      println(s"The original size was ${l.getSize}")
      return None
    }
    println(s"${filtered.take(100)} ...")

    Some(GeneList(l.getName, filtered))
  }

  /**
   * Add a probe list to InterMine by first mapping it into genes (lazily)
   */
  def addProbeList(ls: ListService, list: GeneList, replace: Boolean,
    tags: Seq[String] = Seq("panomicon")): Option[ItemList] =
    addEntrezList(ls,
      list.copy(items =
        platforms.resolve(list.items).flatMap(_.genes.map(_.identifier))
      ),
      replace, tags)

  /**
   *  Add a list of NCBI/Entrez genes to InterMine.
   *  @param ls the ListService
   *  @param list the genes (entrez IDs) to be added
   *  @param replace whether any existing list with the same name should be replaced
   *  @param tags any tags to be added to the list (stored on the InterMine server)
   *  @return the ItemList if it was successfully created, or None if the operation failed.
   */
  def addEntrezList(ls: ListService, list: GeneList, replace: Boolean,
                    tags: Seq[String] = Seq("panomicon")): Option[ItemList] = {

    for {existingList <- Option(ls.getList(list.name))} {
      if (replace) {
        println(s"Delete list $existingList for replacement")
        ls.deleteList(existingList)
      } else {
        //The list existed and replacement was not requested, so we can't proceed.
        //Could report this message to the user somehow
        println(s"Not exporting list '${list.name}' since it already existed (replacement not requested)")
        //throw new IntermineException(
        //  s"Unable to add list, ${name.get} already existed and replacement not requested
        return None
      }
    }

    val ci = new ls.ListCreationInfo("Gene", list.name)
    //Note: we have the option of doing a fuzzy (e.g. symbol-based) export here
    ci.setContent(list.items.asJava)
    ci.addTags(tags.asJava)
    println(s"Exporting list '${list.name}'")
    Some(ls.createList(ci))
  }

  def exportLists(user: String, pass: String, lists: Iterable[GeneList], replace: Boolean): Unit =
    try {
      val ls = getListService(Some(user), Some(pass))
      for { list <- lists } {
        addProbeList(ls, list, replace)
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new IntermineException(s"$e")
    }

  /** Import gene lists.
   * @return pairs of (list items, name). */
  def importLists(user: String, pass: String): Iterable[GeneList] = {
  // Task: pass in a preferred species, get status info back
    try {
      val ls = getListService(Some(user), Some(pass))
      val imLists = ls.getAccessibleLists.asScala

      println("Accessible lists: ")
      for {iml <- imLists} {
        println(s"${iml.getName} ${iml.getType} ${iml.getSize}")
      }

      for { iml <- imLists
           if iml.getType == "Gene"
           list <- asTGList(iml)
           } yield list
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new IntermineException(s"$e")
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
