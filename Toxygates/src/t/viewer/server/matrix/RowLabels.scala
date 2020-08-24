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

package t.viewer.server.matrix

import t.Context
import t.common.shared.sample.ExpressionRow
import t.platform.{Probe, Species}

/**
 * Row labels for normal, single-species matrices.
 */
class RowLabels(context: Context) {
  val probes = context.probeStore

  private def loadProbes(rows: Iterable[ExpressionRow]) =
    if (rows.isEmpty) {
      Seq()
    } else {
      probes.withAttributes(rows.flatMap(r => r.getAtomicProbes.map(Probe(_))))
    }

  /**
   * Dynamically obtains annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  def insertAnnotations(rows: Seq[ExpressionRow], withSymbols: Boolean): Seq[ExpressionRow] = {
    if (!withSymbols) {
      val giMap = Map() ++ loadProbes(rows).map(x =>
        (x.identifier -> x.genes.map(_.identifier).toArray))

      //Only insert geneIDs, leave other data intact.
      rows.map(or => {
        new ExpressionRow(or.getProbe, or.getAtomicProbes, or.getAtomicProbeTitles,
          or.getAtomicProbes.flatMap(giMap(_)),
          or.getGeneSyms, or.getValues)
      })
    } else {
      val pm = Map() ++ loadProbes(rows).map(a => (a.identifier -> a))
      println(pm.take(5))
      rows.map(or => processRow(pm, or))
    }
  }

  def processRow(pm: Map[String, Probe], r: ExpressionRow): ExpressionRow = {
    val atomics = r.getAtomicProbes()
    val ps = atomics.flatMap(pm.get(_))
    assert(ps.size == 1)
    val p = atomics(0)
    val pr = pm.get(p).toArray
    new ExpressionRow(p, pr.flatMap(_.titles),
      pr.flatMap(_.genes.map(_.identifier)), pr.flatMap(_.symbols),
      r.getValues)
  }
}

/**
 * Row labels for orthologous matrices.
 */
class MergedRowLabels(context: Context) extends RowLabels(context) {

  private def repeatStrings[T](xs: Array[T]) =
    withCount(xs).map(x => s"${x._1} (${prbCount(x._2)})")

  //Count the occurrences of each item
  private def withCount[T](xs: Array[T]) =
    xs.groupBy(x => x).toArray.map(x => (x._1, x._2.size))

  private def prbCount(n: Int) = {
    if (n == 0) {
      "No probes"
    } else if (n == 1) {
      "1 probe"
    } else {
      s"$n probes"
    }
  }

  override def processRow(pm: Map[String, Probe], r: ExpressionRow): ExpressionRow = {
    val atomics = r.getAtomicProbes()
    val ps = atomics.flatMap(pm.get(_))

    def speciesPrefix(pf: String) =
      Species.forKnownPlatform(pf).map(_.shortCode).getOrElse("???")

    val expandedGenes = ps.flatMap(p =>
      p.genes.map(g => (speciesPrefix(p.platform), g.identifier)))
    val expandedSymbols = ps.flatMap(p =>
      p.symbols.map(speciesPrefix(p.platform) + ":" + _))

    val nr = new ExpressionRow(atomics.mkString("/"),
      atomics, repeatStrings(ps.map(p => p.name)),
      expandedGenes.map(_._2).distinct,
      repeatStrings(expandedSymbols),
      r.getValues)

    val gils = withCount(expandedGenes).map(x =>
      s"${x._1._1 + ":" + x._1._2} (${prbCount(x._2)})")
    nr.setGeneIdLabels(gils)
    nr
  }
}
