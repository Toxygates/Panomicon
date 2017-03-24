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

import t.db.IndexDB
import t.db.IndexDBWriter
import java.nio.ByteBuffer
import java.nio.charset.Charset
import scala.io.Source
import kyotocabinet.DB
import t.global.KCDBRegistry

object KCIndexDB {
  val c1g = 1l * 1204 * 1204 * 1024
  val c20g = 20l * c1g

  //100k buckets (approx 10% of size, assuming 1 million entries), 1 GB page cache
  //default alignment (8 bytes)
  val options = s"#msiz=$c1g#bnum=100000#pccap=$c1g"

  /**
   * Test program, takes DB file and input key file to store
   */
  def main(args: Array[String]) {
    val db = apply(args(0), true)
    val lines = Source.fromFile(args(1)).getLines.toVector
    for (l <- lines) {
      val k = db.put(l)
      val k2 = db.put("enum", l)
      println(s"$l -> $k, $k2")
    }

    val idxs = (0 to 10).map(x => (Math.random() * lines.size).toInt)
    val em = db.enumMap("enum")

    for (
      i <- idxs; l = lines(i);
      l1 = db.get(lines(i)); l2 = em(lines(i))
    ) {
      println(s"Lookup $l -> $l1, $l2")
    }

    for (l <- lines) {
      db.remove(l)
    }
    db.release
  }

  def readOnce(file: String): Map[String, Int] = {
    var db: KCIndexDB = null
    try {
      db = apply(file, false)
      db.fullMap
    } catch {
      case e: Exception =>
        //TODO permanent solution for non-existent files
        println("Exception while trying to open " + file)
        e.printStackTrace()
        Map()
    } finally {
      if (db != null) {
        db.release
      }
    }
  }

  def apply(file: String, writeMode: Boolean): KCIndexDB = {
    val db = KCDBRegistry.get(file, writeMode)
    db match {
      case Some(d) => new KCIndexDB(d, writeMode)
      case None    => throw new Exception("Unable to get DB")
    }
  }
}

class KCIndexDB(db: DB, writeMode: Boolean)
  extends KyotoCabinetDB(db, writeMode) with IndexDBWriter {

  val nextKey = "##next"

  val utf8 = Charset.forName("UTF-8")
  protected def formKey(x: String): Array[Byte] = x.getBytes(utf8)
  protected def formKey(enum: String, x: String): Array[Byte] = formKey("##" + enum + "##" + x)

  protected def extractKey(data: Array[Byte]): String = new String(data, utf8)
  //TODO

  protected def formValue(value: Int): Array[Byte] = {
    val r = ByteBuffer.allocate(4)
    r.putInt(value)
    r.array()
  }

  protected def extractValue(data: Array[Byte]): Int = {
    val r = ByteBuffer.wrap(data)
    r.getInt()
  }

  private def readNextId(k: Array[Byte]): Int = {
    get(k) match {
      case Some(d) => extractValue(d)
      case None =>
        db.set(k, formValue(0))
        0
    }
  }

  protected var nextId: Int =
    readNextId(formKey(nextKey))

  protected def nextId(enum: String): Int = {
    val r = readNextId(formKey("##" + enum + "#next"))
    r
  }

  val tooLargeMsg = "SEVERE ERROR: The database is too large. Unable to continue."
  protected def incrementNext(): Unit = synchronized {
    db.set(formKey(nextKey), formValue(nextId + 1))
    nextId += 1
    if (nextId == Int.MaxValue) {
      Console.err.println(tooLargeMsg)
      throw new Exception(tooLargeMsg)
    }
  }

  protected def incrementNext(enum: String): Unit = synchronized {
    val k = formKey("##" + enum + "#next")
    val nextId = readNextId(k) + 1
    db.set(k, formValue(nextId))
    if (nextId == Int.MaxValue) {
      Console.err.println(tooLargeMsg)
      throw new Exception(tooLargeMsg)
    }
  }

  /**
   * Retrieve the entire map encoded by this IndexDB.
   */
  def fullMap: Map[String, Int] = {
    val cur = db.cursor()
    var r = Map[String, Int]()
    cur.jump() //go to first record
    try {
      var s = cur.get(true)
      while (s != null) {
        val k = extractKey(s(0))
        if (!k.startsWith("##")) {
          r += (k -> extractValue(s(1)))
        }
        s = cur.get(true)
      }
    } finally {
      cur.disable
    }
    r
  }

  def enumMap(enum: String): Map[String, Int] = {
    val cur = db.cursor()
    var r = Map[String, Int]()
    try {
      val pattern = "##" + enum + "##"
      val k = formKey(pattern)
      cur.jump(k)
      var s = cur.get(true)
      var extK = pattern
      while (s != null && extK.startsWith(pattern)) {
        extK = extractKey(s(0))
        if (extK.startsWith(pattern)) {
          r += (extK.substring(pattern.length()) -> extractValue(s(1)))
        }
        s = cur.get(true)
      }
    } finally {
      cur.disable
    }
    r
  }

  def get(key: String): Option[Int] =
    get(formKey(key)).map(extractValue)

  def get(enum: String, key: String): Option[Int] =
    get(formKey(enum, key)).map(extractValue)

  private def put(key: Array[Byte], value: Array[Byte]) {
    if (db.check(key) != -1) {
      throw new Exception(s"Key $key already existed in DB")
    }

    if (!db.set(key, value)) {
      throw new Exception(s"Unable to store mapping $key -> $value in IndexDB")
    }
  }

  def put(key: String): Int = synchronized {
    if (key.startsWith("##")) {
      throw new Exception("Forbidden value " + key + " (must not start with ##)")
    }
    val value = nextId
    val (k, v) = (formKey(key), formValue(value))
    put(k, v)
    incrementNext()
    value
  }

  def put(enum: String, key: String): Int = synchronized {
    if (key.startsWith("##")) {
      throw new Exception("Forbidden value " + key + " (must not start with ##)")
    }
    val value = nextId(enum)
    val (k, v) = (formKey(enum, key), formValue(value))
    put(k, v)
    incrementNext(enum)
    value
  }

  def remove(key: String): Unit = {
    if (!db.remove(formKey(key))) {
      throw new Exception(s"Failed to remove key $key (does it exist?)")
    }
  }

  override def remove(keys: Iterable[String]): Unit = {
    val formed = keys.map(formKey).toArray
    if (db.remove_bulk(formed, true) == -1) {
      throw new Exception("Bulk removal from KCIndexDB failed")
    }
  }
}
