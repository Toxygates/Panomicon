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

package t.db
import scala.language.postfixOps

trait MatrixContext {
  def probeMap: ProbeMap
  def sampleMap: SampleMap

  def enumMaps: Map[String, Map[String, Int]]

  lazy val reverseEnumMaps = enumMaps.map(x => x._1 -> (Map() ++ x._2.map(_.swap)))

  def absoluteDBReader: MatrixDBReader[ExprValue]
  def foldsDBReader: MatrixDBReader[PExprValue]
  def seriesBuilder: SeriesBuilder[_]
}

/**
 * A database of samples.
 * The database will be opened when returned by its constructor.
 * The database must be closed after use.
 */
trait MatrixDBReader[+E >: Null <: ExprValue] {

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
   * Read all values for a given sample.
   * This routine is optimised for the case of accessing many probes in
   * each array.
   * Probes must be sorted.
   */
  def valuesInSample(x: Sample, probes: Iterable[Int], inverseTransform: Boolean = false): Iterable[E]

  //  def presentValuesInSample(x: Sample): Iterable[(Int, E)] =
  //    valuesInSample(x).filter(_._2.present)

  def valuesInSamples(xs: Iterable[Sample], probes: Iterable[Int]) = {
    val sk = probes.toSeq.sorted
    xs.par.map(valuesInSample(_, sk)).seq
  }

  /**
   * Read all values for a given probe and for a given set of samples.
   * This routine is optimised for the case of accessing a few probes in
   * each array.
   * Samples must be ordered (see sortSamples above)
   */
  def valuesForProbe(probe: Int, xs: Seq[Sample], inverseTransform: Boolean = false): Iterable[(Sample, E)]

  def presentValuesForProbe(probe: Int,
    samples: Seq[Sample]): Iterable[(Sample, E)] =
    valuesForProbe(probe, samples).filter(_._2.present)

  /**
   * Release the reader.
   */
  def release(): Unit

  def emptyValue(probe: String): E

  def emptyValue(pm: ProbeMap, probe: Int): E = {
    val pname = pm.unpack(probe)
    emptyValue(pname)
  }

  //(This version may be suitable later, when a smart adjoin operation is added
  //to the managed matrix.)
  //  def valuesForSamplesAndProbes(xs: Seq[Sample], probes: Seq[Int],
  //    sparseRead: Boolean = false, presentOnly: Boolean = false): Vector[Seq[E]] = {
  //
  //    println("Load for probe count: " + probes.size)
  //    var rows:Seq[Seq[Option[E]]] = if (sparseRead) {
  //      (0 until probes.size).par.map(j => {
  //        val dat = Map() ++ valuesForProbe(probes(j), xs).filter(!presentOnly || _._2.present)
  //        val row = Vector() ++ xs.map(dat.get)
  //        row
  //      }).seq
  //    } else {
  //      //not sparse read, go sample-wise
  //      val cols = xs.par.map(bc => {
  //        //probe to expression
  //        val dat = Map() ++ (valuesInSample(bc).map(x => (x._1 -> x._2))).filter(!presentOnly || _._2.present)
  //        val col = probes.map(dat.get)
  //        col
  //      }).seq
  //      //transpose
  //      Vector.tabulate(probes.size, xs.size)((p, s) => cols(s)(p))
  //    }
  //(filter here)
  //    rows.toVector.map(r => {
  //      val pr = r.find(_ != None).get.get.probe
  //      r.map(_.getOrElse(emptyValue(pr.id)))
  //    })
  //  }

  /**
   * Get values by probes and samples.
   * Samples should be sorted prior to calling this method (using sortSamples above).
   * The result is always a probe-major, sample-minor matrix.
   * The ordering of rows in the result is not guaranteed.
   * The ordering of columns is guaranteed.
   * @param sparseRead if set, we use an algorithm that is optimised
   *  for the case of reading only a few values from each sample.
   *  @param presentOnly if set, samples whose call is 'A' are replaced with the
   *  empty value.
   */
  def valuesForSamplesAndProbes(xs: Seq[Sample], probes: Seq[Int],
    sparseRead: Boolean = false, presentOnly: Boolean = false, inverseTransform: Boolean = false): Vector[Seq[E]] = {

    val ps = probes.filter(probeMap.keys.contains(_)).sorted

    val rows = (if (sparseRead) {
      probes.par.map(p => {
        val dat = Map() ++ valuesForProbe(p, xs, inverseTransform).filter(!presentOnly || _._2.present)
        val row = Vector() ++ xs.map(bc => dat.getOrElse(bc, emptyValue(probeMap, p)))
        row
      }).seq

    } else {
      import scala.collection.mutable.{ Seq => MSeq }
      var rows = Map[String, MSeq[E]]()
      for (p <- ps) {
        rows += probeMap.unpack(p) -> MSeq.fill[E](xs.size)(null)
      }

      //not sparse read, go sample-wise
      (xs zipWithIndex).par.foreach(bc => {
        //probe to expression
        var vs = valuesInSample(bc._1, ps, inverseTransform).filter(!presentOnly || _.present)
        rows.synchronized {
          for (v <- vs) {
            rows(v.probe)(bc._2) = v
          }
          for (p <- rows.keys) {
            if (rows(p)(bc._2) == null) {
              rows(p)(bc._2) = emptyValue(p)
            }
          }
        }
      })
      probes.map(p => rows(probeMap.unpack(p)))
    })
    rows.toVector
  }
}

trait MatrixDBWriter[E <: ExprValue] {

  /**
   * Write a value to the database, keyed by sample and probe.
   * Inserts the value or replaces it if the key already existed.
   */

  def write(s: Sample, probe: Int, e: E): Unit

  def deleteSample(s: Sample): Unit

  /**
   * Close the database.
   */
  def release(): Unit
}

trait MatrixDB[+ER >: Null <: ExprValue, EW <: ExprValue] extends MatrixDBReader[ER] with MatrixDBWriter[EW]
