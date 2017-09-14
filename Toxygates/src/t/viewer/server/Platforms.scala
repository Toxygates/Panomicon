/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.viewer.server

import t.platform.Probe

import t.sparql.Probes

object Platforms {
  def apply(probes: Probes): Platforms = {
    //TODO load platforms incrementally - this is too big and slow
    //Alternatively, speed up this query
    val pps = Probes.platformsAndProbes(probes)
    new Platforms(pps.map(x => x._1 -> x._2.toSet))
  }
}

/**
 * A probe and platform registry. Caches data to avoid heavy sparql queries.
 */
class Platforms(val data: Map[String, Set[Probe]]) {
  //map platform to probe sets
  lazy val platformSets = data.mapValues(_.map(_.identifier))

  //map ID to probe
  lazy val identifierLookup =
    Map() ++ data.toSeq.flatMap(_._2.toSeq).map(x => x.identifier -> x)

  lazy val geneLookup = {
    val raw = (for (
        (pf, probes) <- data.toSeq;
        pr <- probes;
        gene <- pr.genes
      ) yield (gene, pr))
    Map() ++ raw.groupBy(_._1).mapValues(_.map(_._2))
  }

//TODO: update mechanism

  def resolve(identifiers: Seq[String]): Seq[Probe] =
    identifiers.flatMap(identifierLookup.get(_))

  /**
   * Filter probes for a number of platforms.
   */
  def filterProbes(probes: Iterable[String],
      platforms: Iterable[String]): Iterable[String] = {
    var rem = Set() ++ probes
    var r = Set[String]()
    for (p <- platforms; valid = filterProbes(rem, p)) {
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
   * Filter probes for one platform.
   */
  def filterProbes(probes: Iterable[String], platform: String): Iterable[String] = {
    if (probes.size == 0) {
      data(platform).toSeq.map(_.identifier)
    } else {
      val pset = probes.toSet
      println(s"Filter (${pset.size}) ${pset take 20} ...")
      val r = pset.intersect(platformSets(platform))
      println(s"Result (${r.size}) ${r take 20} ...")
      r.toSeq
    }
  }
}
