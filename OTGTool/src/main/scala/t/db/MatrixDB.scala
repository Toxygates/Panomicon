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

package t.db

import t.OTGSeries

import scala.Vector
import scala.language.postfixOps
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.model.sample.Attribute
import t.model.sample.CoreParameter

import scala.reflect.ClassTag

object MatrixDB {
  def get(file: String, writeMode: Boolean)(implicit context: MatrixContext): MatrixDB[PExprValue, PExprValue] = {
    KCChunkMatrixDB.apply(file, writeMode)
  }
}

trait MatrixContext {
  def probeMap: ProbeMap
  def sampleMap: SampleMap

  def enumMaps(attrib: Attribute): Map[String, Int] = enumMaps(attrib.id)
  def enumMaps: Map[String, Map[String, Int]]

  lazy val reverseEnumMaps = enumMaps.map(x => x._1 -> (Map() ++ x._2.map(_.swap)))

  def absoluteDBReader: MatrixDBReader[PExprValue]
  def foldsDBReader: MatrixDBReader[PExprValue]
  def timeSeriesDBReader: SeriesDB[OTGSeries]
  def doseSeriesDBReader: SeriesDB[OTGSeries]

  def timeSeriesBuilder: SeriesBuilder[_]
  def doseSeriesBuilder: SeriesBuilder[_]

  def probeSets: Map[String, Seq[Int]] = Map()

  /**
   * Probes expected to be present in the database for a given sample.
   * They are not guaranteed to actually be present.
   */
  def expectedProbes(x: Sample) = {
    val plat = x(CoreParameter.Platform)

    probeSets.getOrElse(plat,
      throw new Exception(s"Expected probes are not defined for the platform $plat"))
  }
}

/**
 * A database of samples.
 * The database will be opened when returned by its constructor.
 * The database must be closed after use.
 */
trait MatrixDBReader[E <: ExprValue] {

  implicit def probeMap: ProbeMap

  /**
   * Read all samples stored in the database.
   */
  def allSamples: Iterable[Sample]

  /**
   * Sort samples in an order that is optimised for sequential database access.
   * Useful for repeated calls to valuesInSample.
   */
  def sortSamples(xs: Iterable[Sample]): Seq[Sample]

  /**
   * Sort probes in an order that is optimised for valuesInSample.
   */
  def sortProbes(probes: Iterable[Int]): Seq[Int] =
    probes.toSeq.sorted

  /**
   * Read all values for a given sample.
   * This routine is optimised for the case of accessing many probes in
   * each array.
   * Probes must be sorted.
   * The size of the returned iterable is the same as the passed in probes, and
   * in the same order.
   * @param padMissingValues If true, empty values will be inserted where none was found in the
   * database.
   */
  def valuesInSample(x: Sample, probes: Seq[Int],
      padMissingValues: Boolean): Iterable[E]

  def valuesInSamples(xs: Iterable[Sample], probes: Seq[Int],
      padMissingValues: Boolean) =
    xs.map(valuesInSample(_, probes, padMissingValues))

  /**
   * Read all values for a given probe and for a given set of samples.
   * This routine is optimised for the case of accessing a few probes in
   * each array.
   * Samples must be ordered (see sortSamples above)
   */
  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, E)]

  def presentValuesForProbe(probe: Int,
    samples: Seq[Sample]): Iterable[(Sample, E)] =
    valuesForProbe(probe, samples).filter(_._2.present)

  /**
   * Release the reader.
   */
  def release(): Unit

  def emptyValue(probe: ProbeId): E

  def emptyValue(pm: ProbeMap, probe: Int): E = {
    val pname = pm.unpack(probe)
    emptyValue(pname)
  }

  /**
   * Get values by probes and samples.
   * Samples should be sorted prior to calling this method (using sortSamples above).
   * The result is always a probe-major, sample-minor matrix.
   * The ordering of rows in the result is guaranteed for non-sparse reads.
   * The ordering of columns is guaranteed.
   * @param sparseRead if set, we use an algorithm that is optimised
   *  for the case of reading only a few values from each sample.
   * @param presentOnly if set, in the sparse case, samples whose call is 'A' are replaced with the
   *  empty value.
   */
  def valuesForSamplesAndProbes(xs: Seq[Sample], probes: Seq[Int],
    sparseRead: Boolean = false, presentOnly: Boolean = false)(implicit tag: ClassTag[E]): Array[Array[E]] = {

    val ps = probes.filter(probeMap.keys.contains(_)).sorted

    val rows = if (sparseRead) {
      probes.par.map(p => {
        val dat = Map() ++ valuesForProbe(p, xs).filter(!presentOnly || _._2.present)
        xs.map(bc => dat.getOrElse(bc, emptyValue(probeMap, p))).toArray
      }).seq.toArray
    } else {
      //not sparse read, go sample-wise
      val rs = xs.par.map(x => {
        valuesInSample(x, ps, true).toSeq
      }).seq.toSeq
      Array.tabulate(ps.size, xs.size)((p, x) =>
        rs(x)(p))
    }
    rows.toArray
  }
}

trait MatrixDBWriter[E <: ExprValue] {

  /**
   * Write a value to the database, keyed by sample and probe.
   * Inserts the value or replaces it if the key already existed.
   */

  def write(s: Sample, probe: Int, e: E): Unit

  def writeMany(s: Sample, vs: Iterable[(Int, E)]): Unit = {
    for (v <- vs) {
      write(s, v._1, v._2)
    }
  }

  def deleteSample(s: Sample): Unit

  def deleteSamples(ss: Iterable[Sample]): Unit = {
    for (s <- ss) {
      deleteSample(s)
    }
  }

  /**
   * Close the database.
   */
  def release(): Unit
}

trait MatrixDB[ER <: ExprValue, EW <: ExprValue] extends MatrixDBReader[ER] with MatrixDBWriter[EW]
