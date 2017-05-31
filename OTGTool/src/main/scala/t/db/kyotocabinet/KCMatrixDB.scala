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

package t.db.kyotocabinet

import t.db.MatrixDB
import kyotocabinet.DB
import java.nio.ByteBuffer
import t.db.ExprValue
import t.db.BasicExprValue
import t.db.PExprValue
import t.db.ExtMatrixDB
import otg.Species._
import otg.Context
import t.platform.SimpleProbe
import t.db.MatrixContext
import t.db.Sample
import t.global.KCDBRegistry
import t.platform.Probe
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.db.MatrixDBReader
import t.db.MatrixDBWriter

object KCMatrixDB {

  def get(file: String, writeMode: Boolean)(implicit context: MatrixContext): KCMatrixDB = {
    val db = KCDBRegistry.get(file, writeMode)
    db match {
      case Some(d) => new KCMatrixDB(d, writeMode)
      case None => throw new Exception("Unable to get DB")
    }
  }

  def getExt(file: String, writeMode: Boolean)(implicit context: MatrixContext): MatrixDB[PExprValue, PExprValue] = {
    import KCChunkMatrixDB._
    if (file.startsWith(CHUNK_PREFIX)) {
      KCChunkMatrixDB.apply(removePrefix(file), writeMode)
    } else {
      val db = KCDBRegistry.get(file, writeMode)
      db match {
        case Some(d) => new KCExtMatrixDB(d, writeMode)
        case None    => throw new Exception("Unable to get DB")
      }
    }
  }
}

/**
 * The database format used for normal data storage,
 * indexed by sample ID and probe.
 *
 */
abstract class AbstractKCMatrixDB[E >: Null <: ExprValue](db: DB, writeMode: Boolean)
(implicit val context: MatrixContext)
  extends KyotoCabinetDB(db, writeMode) with MatrixDB[E, E] {

  /**
   * Key size: 8 bytes
   */
  protected def formKey(s: Sample, probe: Int): Array[Byte] = {
    val r = ByteBuffer.allocate(8)
    r.putInt(s.dbCode)
    r.putInt(probe)
    r.array()
  }

  /**
   * Returns (sample ID, probe)
   */
  protected def extractKey(data: Array[Byte]): (Int, Int) = {
    val b = ByteBuffer.wrap(data)
    (b.getInt, b.getInt)
  }

  protected def tryExtractSampleId(data: Array[Byte]): Option[Int] = {
    val b = ByteBuffer.wrap(data)
    if (data.length >= 4) {
      Some(b.getInt)
    } else {
      None
    }
  }

  protected def formValue(value: E): Array[Byte]

  protected def extractValue(data: Array[Byte], probe: String): E

  def allSamples: Vector[Sample] = allSamples(false, Set()).map(_._2)

  /**
   * Testing/maintenance operation.
   * Returns all samples, and optionally deletes a set of sample IDs
   * during the traversal.
   */
  def allSamples(verbose: Boolean, deleteSet: Set[Int]): Vector[(Int, Sample)] = {
    var r: Vector[(Int, Sample)] = Vector.empty
    println(s"Deleting ${deleteSet.size} samples")
    val cur = db.cursor()
    try {
      var continue = cur.jump()
      var lastCode = -1
      var count = 0
      var removed = 0
      while (continue) {
        val k = cur.get_key(false)
        count += 1
        if (k == null) {
          continue = false
        } else {
          val oi = tryExtractSampleId(k)
          if (oi == None) {
            //broken record
            cur.remove()
          } else {
            val i = oi.get
            if (deleteSet.contains(i)) {
              cur.remove()
              removed += 1
            } else {
              cur.step()
            }

            if (lastCode != i) {
              lastCode = i
              r :+= (i, Sample(i))
              if (verbose) {
                println(s"$count samples")
                println(s"Begin sample ${i}")
                count = 0
              }
            }
          }
        }
      }
      println(s"Removed $removed")
    } finally {
      cur.disable
    }
    r.reverse
  }

  private lazy val pmap = context.probeMap

  //TODO consider removing/encapsulating
  def sortSamples(ss: Iterable[Sample]): Seq[Sample] = ss.toList.sortWith(_.dbCode < _.dbCode)

  def valuesInSample(x: Sample, keys: Iterable[Int]): Iterable[E] = {
    var r: Vector[E] = Vector()
    println(x.identifier + " (" + keys.size + ")")

    val useKeys = if (!keys.isEmpty) { keys } else { pmap.keys.toSeq.sorted }

    for (k <- useKeys; lookup = formKey(x, k);
      unpacked = pmap.unpack(k)) {
      val data = db.get(lookup)
      if (data == null) {
        r :+= emptyValue(unpacked)
      } else {
        r :+= extractValue(data, unpacked)
      }
      r
    }
    r
  }

  def deleteSample(x: Sample): Unit = {
    val allKeys = pmap.keys.toSeq.map(p => formKey(x, p)).toArray
    if (db.remove_bulk(allKeys, true) == -1) {
      throw new Exception("KCMatrixDB bulk removal failed")
    }
  }

  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, E)] = {
    var r: Vector[(Sample, E)] = Vector.empty
    println("Requested probe " + probe + " for barcodes " + xs)
    val probeName = pmap.unpack(probe)
    val keys = xs.map(x => formKey(x, probe))
    val data = db.get_bulk(keys.toArray, false)

    if (data != null) {
      // keys and values are interleaved in the array returned by get_bulk
      for (i <- Range(0, data.length, 2)) {
        val k = data(i)
        val v = data(i + 1)
        val extr = extractKey(k)
        r :+= (Sample(extr._1), extractValue(v, probeName))
      }
    } else {
      println("valuesForProbe: data was null")
    }
    r
  }

  def write(s: Sample, probe: Int, e: E) {
    val key = formKey(s, probe)
    val v = formValue(e)
    db.set(key, v)
  }

  def dumpKeys(froms: String, fromp: String, len: Int): Unit = {
    val cur = db.cursor()
    val smap = context.sampleMap
    println(smap.keys.take(10))
    println(smap.tokens.take(10))
    println(pmap.keys.take(10))
    println(pmap.tokens.take(10))
    val k = formKey(Sample(froms), pmap.pack(fromp))
    cur.jump(k)

    for (i <- 0 until len) {
      val k = cur.get_key(true)
      val x = extractKey(k)
      println(x._1 + "/" + smap.tryUnpack(x._1) + " :: " + x._2 + "/" + pmap.tryUnpack(x._2))
    }
    cur.disable()
  }
}

class KCMatrixDB(db: DB, writeMode: Boolean)(implicit context: MatrixContext)
  extends AbstractKCMatrixDB[BasicExprValue](db, writeMode) {

  implicit val probeMap = context.probeMap

  protected def extractValue(data: Array[Byte], probe: String) = {
    val b = ByteBuffer.wrap(data)
    val x = b.getDouble
    BasicExprValue(x, b.getChar, probe)
  }

  /**
   * Value size: 10 bytes
   */
  protected def formValue(v: BasicExprValue): Array[Byte] = {
    val r = ByteBuffer.allocate(10)
    r.putDouble(v.value)
    if (!(v.call == 'A' || v.call == 'P' || v.call == 'M')) {
      throw new Exception(s"Invalid call code: ${v.call} - expected P/M/A")
    }
    r.putChar(v.call)
    r.array
  }

  def emptyValue(probe: String) = BasicExprValue(0.0, 'A', probe)
}

class KCExtMatrixDB(db: DB, writeMode: Boolean)(implicit context: MatrixContext)
  extends AbstractKCMatrixDB[PExprValue](db, writeMode) with ExtMatrixDB {

  implicit val probeMap = context.probeMap

  protected def extractValue(data: Array[Byte], probe: String) = {
    val b = ByteBuffer.wrap(data)
    val x = b.getDouble
    val call = b.getChar
    val p = b.getDouble

    PExprValue(x, p, call, probe)
  }

  /**
   * Value size: 18 bytes
   */
  protected def formValue(v: PExprValue): Array[Byte] = {
    val r = ByteBuffer.allocate(18)
    r.putDouble(v.value)
    if (!(v.call == 'A' || v.call == 'P' || v.call == 'M')) {
      throw new Exception(s"Invalid call code: ${v.call} - expected P/M/A")
    }
    r.putChar(v.call)
    r.putDouble(v.p)
    r.array
  }
}
