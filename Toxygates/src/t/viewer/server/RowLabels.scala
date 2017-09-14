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

import t.Context
import t.common.shared.DataSchema
import t.common.shared.sample.ExpressionRow
import t.platform.Probe

class RowLabels(context: Context, schema: DataSchema) {
  val probes = context.probes

  /**
   * Dynamically obtain annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  def insertAnnotations(rows: Seq[ExpressionRow]): Seq[ExpressionRow] = {
    val allAtomics = rows.flatMap(_.getAtomicProbes)
    val attribs = probes.withAttributes(allAtomics.map(Probe(_)))
    val pm = Map() ++ attribs.map(a => (a.identifier -> a))
    println(pm.take(5))

    rows.map(or => processRow(pm, or))
  }

  def processRow(pm: Map[String, Probe], r: ExpressionRow): ExpressionRow = {
    val atomics = r.getAtomicProbes()
    val ps = atomics.flatMap(pm.get(_))
    assert(ps.size == 1)
    val p = atomics(0)
    val pr = pm.get(p)
    new ExpressionRow(p,
      pr.map(_.name).getOrElse(""),
      pr.toArray.flatMap(_.genes.map(_.identifier)),
      pr.toArray.flatMap(_.symbols.map(_.symbol)),
      r.getValues)
  }

}

class MergedRowLabels(context: Context, schema: DataSchema) extends RowLabels(context, schema) {

  private def repeatStrings[T](xs: Seq[T]): Iterable[String] =
    withCount(xs).map(x => s"${x._1} (${prbCount(x._2)})")

  //this is probably quite inefficient
  private def withCount[T](xs: Seq[T]): Iterable[(T, Int)] =
    xs.distinct.map(x => (x, xs.count(_ == x)))

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
    val expandedGenes = ps.flatMap(p =>
      p.genes.map(g => (schema.platformSpecies(p.platform), g.identifier)))
    val expandedSymbols = ps.flatMap(p =>
      p.symbols.map(schema.platformSpecies(p.platform) + ":" + _.symbol))

    val nr = new ExpressionRow(atomics.mkString("/"),
      atomics,
      repeatStrings(ps.map(p => p.name)).toArray,
      expandedGenes.map(_._2).distinct,
      repeatStrings(expandedSymbols).toArray,
      r.getValues)

    val gils = withCount(expandedGenes).map(x =>
      s"${x._1._1 + ":" + x._1._2} (${prbCount(x._2)})").toArray
    nr.setGeneIdLabels(gils)
    nr
  }
}
