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

package t.db.kyotocabinet.chunk

import t.db.PExprValue
import t.db.MatrixDB
import t.db.ExprValue
import t.db.ProbeMap
import t.db.Sample
import t.db.MatrixContext
import java.nio.ByteBuffer
import t.db.ExtMatrixDB
import t.global.KCDBRegistry
import kyotocabinet.DB
import t.db.kyotocabinet.KyotoCabinetDB

/**
 * A vector-chunk of adjacent values in a vector in the matrix.
 * @param sample the sample this chunk belongs to.
 * @param start the probe where this chunk's contiguous range begins.
 */
case class VectorChunk[E <: ExprValue] (sample: Int, start: Int, xs: Seq[(Int, E)]) {

  assert (start % CHUNKSIZE == 0)

  /**
   * The probes in this chunk, in order.
   */
  def probes: Seq[Int] =
    xs.map(_._1)

  /**
   * Write the given probe's value, or create it if it doesn't exist.
   */
  def insert(p: Int, x: E): VectorChunk[E] = {
    val nxs = (xs.takeWhile(_._1 < p) :+ (p, x)) ++ xs.dropWhile(_._1 <= p)

    assert(nxs.size <= CHUNKSIZE)
    VectorChunk(sample, start, nxs)
  }

  /**
   * Remove the given probe's value.
   */
  def remove(p: Int): VectorChunk[E] = {
    val nxs = xs.takeWhile(_._1 < p) ++ xs.dropWhile(_._1 <= p)
    VectorChunk(sample, start, nxs)
  }
}

object KCChunkMatrixDB {
  val CHUNK_PREFIX = "kcchunk:"

  def removePrefix(file: String) = file.split(CHUNK_PREFIX)(1)

  def apply(file: String, writeMode: Boolean)(implicit context: MatrixContext) = {
    val db = KCDBRegistry.get(file, writeMode)
    db match {
      case Some(d) =>
        new KCChunkMatrixDB(d)
      case None => throw new Exception("Unable to get DB")
    }
  }
}

/**
 * Chunked matrix DB.
 * Key size: 8 bytes (sample + probe)
 * Value size: 22b * chunksize (2816b at 128 probe chunks)
 * Expected number of records: 5-10 million
 */
class KCChunkMatrixDB(db: DB)(implicit mc: MatrixContext)
  extends KyotoCabinetDB(db) with ExtMatrixDB {

  type V = VectorChunk[PExprValue]

  protected def formKey(vc: V): Array[Byte] =
    formKey(vc.sample, vc.start)

  protected def formKey(s: Int, p: Int): Array[Byte] = {
    val r = ByteBuffer.allocate(8)
    r.putInt(s)
    r.putInt(p)
    r.array()
  }

  protected def extractKey(data: Array[Byte]): (Int, Int) = {
    val b = ByteBuffer.wrap(data)
    (b.getInt, b.getInt)
  }

  protected def formValue(vc: V): Array[Byte] = {
    val r = ByteBuffer.allocate(vc.xs.size * (4 + (8 + 8 + 2)))
    for (x <- vc.xs) {
      r.putInt(x._1) //probe
      r.putDouble(x._2.value)
      r.putDouble(x._2.p)
      r.putChar(x._2.call)
    }
    r.array()
  }

  protected def extractValue(sample: Int, start: Int,
      data: Array[Byte]): V = {
    val b = ByteBuffer.wrap(data)
    var r = Seq[(Int, PExprValue)]()
    while(b.hasRemaining()) {
      val pr = b.getInt
      val x = b.getDouble
      val p = b.getDouble
      val c = b.getChar
      r +:= (pr, PExprValue(x, p, c))
    }
    VectorChunk(sample, start, r)
  }

  /**
   * Read all chunk keys as sample,probe-pairs
   */
  private def allChunks: Iterable[(Int, Int)] = {
    val cur = db.cursor()
    var continue = cur.jump()
    var r = Vector[(Int, Int)]()
    while (continue) {
      val key = cur.get_key(true)
      if (key != null) {
        val ex = extractKey(key)
        r :+= (ex._1, ex._2)
      } else {
        continue = false
      }
    }
    r
  }

  private def chunkStartFor(p: Int): Int = {
    (p / CHUNKSIZE) * CHUNKSIZE
  }

  /**
   * Find the chunk that contains the given sample/probe pair, or create a
   * blank chunk if it doesn't exist.
   */
  private def findOrCreateChunk(sample: Int, probe: Int): V = {
    val start = chunkStartFor(probe)
    val key = formKey(sample, start)
    val v = db.get(key)
    if (v == null) {
      new VectorChunk[PExprValue](sample, start, Seq())
    } else {
      extractValue(sample, start, v)
    }
  }

  /**
   * Write the (possibly new) chunk into the database, overwriting it
   * if it already existed. If there are no values in it, it will be
   * removed.
   */
  private def updateChunk(v: V): Unit = {
    val key = formKey(v)

    if (v.xs.size == 0) {
      db.remove(key)
    } else {
      val value = formValue(v)
      if (!db.set(key, value)) {
        throw new Exception("Failed to write value")
      }
    }
  }

  /**
   * Delete a chunk by key.
   */
  private def deleteChunk(sample: Int, probe: Int): Unit = {
    val key = new VectorChunk[PExprValue](sample, probe, Seq())
    updateChunk(key)
  }

  def allSamples: Iterable[Sample] =
    allChunks.map(x => Sample(x._1))

  implicit val probeMap = mc.probeMap

  def sortSamples(xs: Iterable[Sample]): Seq[Sample] =
    xs.toSeq.sortBy(_.dbCode)

  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, PExprValue)] = {
    val probeName = probeMap.unpack(probe)
    val cs = xs.map(x => findOrCreateChunk(x.dbCode, probe))
    for (c <- cs; x <- c.xs; if (x._1 == probe))
      yield (Sample(c.sample), x._2.copy(probe = probeName))
  }

  def valuesInSample(x: Sample, probes: Iterable[Int]): Iterable[PExprValue] = {
    val keys = probes.map(p => chunkStartFor(p)).toSeq.distinct
    val chunks = keys.map(k => findOrCreateChunk(x.dbCode, k))
    val ps = probes.toSet
    for (
      c <- chunks; x <- c.xs;
      if (ps.contains(x._1));
      probeName <- probeMap.tryUnpack(x._1)
    ) yield x._2.copy(probe = probeName)
  }

  def deleteSample(s: Sample): Unit = {
    val x = s.dbCode
    val ds = allChunks.filter(_._1 == x)
    for (d <- ds) {
      deleteChunk(d._1, d._2)
    }
  }

  def write(s: Sample, probe: Int, e: PExprValue): Unit = synchronized {
    val c = findOrCreateChunk(s.dbCode, probe)
    val u = c.insert(probe, e)
    updateChunk(u)
  }
}
