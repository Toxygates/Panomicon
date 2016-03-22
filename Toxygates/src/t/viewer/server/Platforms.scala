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

package t.viewer.server

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
 * A probe and platform registry.
 */
class Platforms(val data: Map[String, Set[String]]) {
//TODO: update mechanism

//  println("Platforms: ")
//  for (p <- data) {
//    println(s"\t${p._1}: ${p._2.size}")
//  }

  /**
   * Filter probes for a number of platforms.
   */
  def filterProbes(probes: Seq[String], platforms: Iterable[String]): Seq[String] =
    platforms.toSeq.flatMap(pf => filterProbes(probes, pf))

  /**
   * Filter probes for all platforms.
   */
  def filterProbesAllPlatforms(probes: Seq[String]): Seq[String] = {
    val pfs = data.keys
    filterProbes(probes, pfs)
  }

  def platformForProbe(p: String): Option[String] =
    data.find(_._2.contains(p)).map(_._1)

  /**
   * Filter probes for one platform.
   */
  def filterProbes(probes: Seq[String], platform: String): Iterable[String] = {
    if (probes.size == 0) {
      data(platform).toSeq
    } else {
      println(s"Filter ${probes}")
      val r = probes.filter(p => data(platform).contains(p))
      println(s"Result $r")
      r
    }
  }
}
