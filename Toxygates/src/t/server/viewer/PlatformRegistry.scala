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

package t.server.viewer

import t.platform.Probe
import t.sparql.{PlatformLoader, ProbeStore}
import t.platform.Species.Species

/**
 * A loader that contains in-memory probes.
 * @param platforms
 */
class MemoryPlatforms(platforms: Map[String, Iterable[Probe]]) extends PlatformLoader {
  def probesForPlatform(platform: String): Iterable[Probe] =
    platforms.getOrElse(platform, Seq())

  def allPlatforms: Map[String, Iterable[Probe]] = platforms
}

class PlatformRegistry(loader: PlatformLoader) {
  //map platform to probe sets
  private lazy val platformSets = loader.allPlatforms.mapValues(_.map(_.identifier).toSet)

  //map ID to probe
  private lazy val identifierLookup =
    Map() ++ loader.allPlatforms.toSeq.flatMap(_._2.toSeq).map(x => x.identifier -> x)

  private var platformIdentifierLookup = Map.empty[String, Map[String, Probe]]
  private def ensurePlatformLoaded(platform: String): Unit = {
    if (!platformIdentifierLookup.contains(platform)) {
      platformIdentifierLookup += (platform ->
        loader.probesForPlatform(platform).iterator.map(p => p.identifier -> p).toMap)
    }
  }

  lazy val allProbes: Iterable[Probe] = loader.allPlatforms.values.toSeq.flatten
  def getProbe(platform: String, id: String): Option[Probe] = {
    ensurePlatformLoaded(platform)
    platformIdentifierLookup(platform).get(id)
  }

  def probeIdentifiers(platform: String): Set[String] =
    loader.probesForPlatform(platform).map(_.identifier).toSet
  def platformProbes(platform: String): Iterable[Probe] =
    loader.probesForPlatform(platform)

  lazy val geneLookup = {
    val raw = (for (
      (pf, probes) <- loader.allPlatforms.toSeq;
      pr <- probes;
      gene <- pr.genes
      ) yield (gene, pr))
    Map() ++ raw.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
   * Probe resolution by going through all known platforms. Slow, forces
   * loading of all platforms (the first time the method is called).
   */
  def resolve(identifiers: Seq[String]): Seq[Probe] =
    identifiers.flatMap(identifierLookup.get(_))

  def resolve(platform: Option[String], identifiers: Seq[String]): Seq[Probe] =
    platform match {
      case Some(pf) => resolve(pf, identifiers)
      case _ => resolve(identifiers)
    }

  /**
   * Probe resolution by going through a single known platform.
   */
  def resolve(platform: String, identifiers: Seq[String]): Seq[Probe] = {
    ensurePlatformLoaded(platform)
    val registry = platformIdentifierLookup(platform)
    identifiers.flatMap(registry.get(_))
  }

  /**
   * Filter probes for a number of platforms.
   */
  def filterProbes(probes: Iterable[String],
      platforms: Iterable[String],
      species: Option[Species] = None): Iterable[String] = {
    var rem = Set() ++ probes
    var r = Set[String]()
    for (p <- platforms; valid = filterProbes(rem, p, species)) {
      rem --= valid
      r ++= valid
    }
    r.toSeq
  }

  /**
   * Filter probes for all platforms.
   */
  def filterProbesAllPlatforms(probes: Seq[String]): Seq[String] =
    probes.filter(identifierLookup.keySet.contains)

  def platformForProbe(p: String): Option[String] =
    platformSets.find(_._2.contains(p)).map(_._1)

  /**
   * Filter probes for one platform. Returns all probes in the platform if the input
   * set is empty.
   */
  def filterProbes(probes: Iterable[String], platform: String,
      species: Option[Species]): Iterable[String] = {
    if (probes.size == 0) {
      allProbes(platform, species)
    } else {
      val pset = probes.toSet
      println(s"Filter (${pset.size}) ${pset take 20} ...")
      val r = pset.intersect(probeIdentifiers(platform))
      println(s"Result (${r.size}) ${r take 20} ...")
      r.toSeq
    }
  }

  private def allProbes(platform: String, species: Option[Species]) = {
    val all = probeIdentifiers(platform)
    if (platform.startsWith("mirbase")) {
      species match {
        case Some(sp) => all.filter(_.startsWith(sp.shortCode))
        case _ => all
      }
    } else {
      all
    }
  }
}
