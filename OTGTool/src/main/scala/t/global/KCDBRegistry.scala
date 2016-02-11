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

package t.global

import kyotocabinet.DB
import scala.collection.mutable.Map

/**
 * A registry that simply opens writers and keeps them all open.
 * Efficient for maintenance mode. Not intended for normal operation.
 */
object SimpleRegistry {
  val writers = Map[String, DB] ()

  def findOrCreate(file: String): DB = synchronized {
    if (writers.contains(file)) {
      writers(file)
    } else {
      val db = KCDBRegistry.openWrite(file)
      writers.put(file, db)
      db
    }
  }

  def closeAll(): Unit = synchronized {
    for ((k, v) <- writers) {
      v.close()
    }
  }
}

/**
 * The KCDBRegistry handles concurrent database requests.
 * The goal is to keep a DB object open as long as possible in a given
 * process, to maximise the benefits of the cache, yet honour incoming
 * write requests from the admin UI.
 *
 * IMPORTANT: only one KCDBRegistry object must be in existence for
 * every given file that must be accessed. Multiple processes/classloaders
 * must not attempt to access the same file through this object.
 */
object KCDBRegistry {

  private var readCount = Map[String, Int]()
  private var inWriting = Set[String]()
  private var readers = Map[String, DB]()
  private val myWriters = new ThreadLocal[Map[String, DB]]()

  //Special mode for maintenance.
  private var maintenance: Boolean = false
  def setMaintenance(maintenance: Boolean) {
    this.maintenance = maintenance
  }

  def isMaintenance: Boolean = maintenance

  /**
   * Remove kyoto cabinet options from file name
   */
  private def realPath(file: String) = file.split("#")(0)

  //To be used at the end if noClose/maintenance mode is activated.
  def closeAll() {
    SimpleRegistry.closeAll()
  }

  def get(file: String, writeMode: Boolean) = {
    if (writeMode) {
      getWriter(file)
    } else {
      getReader(file)
    }
  }

  private def getMyWriter(file: String): Option[DB] = {
    if (myWriters.get == null) {
      None
    } else {
      myWriters.get.get(realPath(file))
    }
  }

  /**
   * Register a thread-local writer for a file.
   * @param db the writer DB to set. If null, then the mapping
   * will be removed.
   */
  private def setMyWriter(file: String, db: DB) = {
    val rp = realPath(file)
    val m = if (myWriters.get == null) {
      val m = Map[String, DB]()
      myWriters.set(m)
      m
    } else {
      myWriters.get
    }
    if (db == null) {
      m -= rp
    } else {
      m += (rp-> db)
    }
  }

  /**
   * Get a reader. The request is granted if there is
   * no existing or pending writer. The reader count is increased.
   * Readers should be released with the release() call after use.
   */
  def getReader(file: String): Option[DB] = synchronized {
    println(s"Read request for $file")
    val rp = realPath(file)
    if (maintenance) {
      return Some(SimpleRegistry.findOrCreate(file))
    }

    if (getMyWriter(rp) != None) {
      if (inWriting.contains(rp)) {
        //this thread is writing
        incrReadCount(rp)
        return getMyWriter(rp)
      } else {
        //was forcibly closed
        setMyWriter(rp, null)
      }
    } else if (inWriting.contains(rp)) {
      println("Writing in progress - reader denied")
      return None
    }
    incrReadCount(rp)
    if (readers.contains(rp)) {
      Some(readers(rp))
    } else {
      val d = openRead(file)
      readers += rp -> d
      Some(d)
    }

  }

  protected[global] def getReadCount(file: String): Int = {
    readCount.getOrElse(realPath(file), 0)
  }

  protected[global] def threadHasWriter(file: String): Boolean = {
    getMyWriter(realPath(file)) != None
  }

  protected[global] def isInWriting(file: String): Boolean = {
    inWriting.contains(realPath(file))
  }

  private def incrReadCount(file: String) {
    val rp = realPath(file)
    val oc = readCount.getOrElse(rp, 0)
    println(s"Read count: $oc")
    readCount += rp -> (oc + 1)
  }

  /**
   * Get a writer. The request is granted if there is no existing or
   * pending writer. The request will block until all current readers
   * (and prior writers in the queue) are finished.
   * The same thread must not already hold a reader.
   * Writers should be released with release() after use.
   */
  def getWriter(file: String): Option[DB] = {
    println(s"Write request for $file")
    if (maintenance) {
      return Some(SimpleRegistry.findOrCreate(file))
    }

    val rp = realPath(file)
    synchronized {
      if (getMyWriter(rp) != None) {
        if (inWriting.contains(rp)) {
          return getMyWriter(rp)
        } else {
          //was forcibly closed
          setMyWriter(rp, null)
          inWriting -= rp
        }
      } else if (inWriting.contains(rp)) {
        println("Writing by other thread in progress - writer denied")
        return None
      }
    }

    var count = 0
    var r: Option[DB] = innerGetWriter(file)
    //sleep at most 20s here
    while (count < 10 && r == None) {
      Thread.sleep(2000)
      count += 1
      r = innerGetWriter(file)
    }
    if (r != None) {
      setMyWriter(rp, r.get)
    } else {
      println("Writer request timed out")
    }
    r
  }

  private def innerGetWriter(file: String): Option[DB] = synchronized {
    val rp = realPath(file)
    val rc = readCount.getOrElse(rp, 0)
    val iw = inWriting.contains(rp)
    println(s"Read count for $file: $rc (waiting for 0) in writing: $iw (waiting for false)")
    if (rc == 0 && !inWriting.contains(rp)) {
      if (readers.contains(rp)) {
        readers(rp).close()
        readers -= rp
      }
      val w = openWrite(file)
      inWriting += rp
      readers += rp -> w
      Some(w)
    } else {
      None
    }
  }

  /**
   * Release a previously obtained reader or writer.
   */
  def release(file: String): Unit = synchronized {
    if (maintenance) {
      return
    }

    val rp = realPath(file)
    if (readCount.getOrElse(rp, 0) > 0) {
      readCount += (rp -> (readCount(rp) - 1))
      //note we could close the DB here but we keep it open
      //for future users
    } else if (inWriting.contains(rp)) {
      //This release request must come from the same thread
      assert(getMyWriter(file) != None)
      getMyWriter(rp).get.close()
      inWriting -= rp
      setMyWriter(rp, null)

    } else {
      System.err.println(s"Warning, incorrect release request - $file was not open")
    }
  }

  //TODO simplify this and this class in general (interacts with algorithms above)
  def forceCloseWriters(): Unit = synchronized {
    val all = inWriting
    for (f <- all) {
      readers(f).close()
    }
    readCount --= all
    readers --= all
    inWriting = Set()
  }

  /**
   * Open the database for reading.
   */
  private[global] def openRead(file: String): DB = {
    println(s"Attempting to open $file for reading")
    val db = new DB()
    if (!db.open(file, DB.OREADER | DB.OCREATE)) {
      throw new Exception("Unable to open db")
    }
    db
  }

  /**
   * Open the database for writing.
   */
  private[global] def openWrite(file: String): DB = {
    println(s"Attempting to open $file for writing")
    val db = new DB()
    if (!db.open(file, DB.OWRITER | DB.OCREATE)) {
      throw new Exception("Unable to open db")
    }
    db
  }

}
