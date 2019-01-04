/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
 * The KCDBRegistry handles concurrent database requests.
 * The goal is to keep a DB object open as long as possible in a given
 * process, to maximise the benefits of the cache, yet honour incoming
 * write requests from the admin UI or user data.
 *
 * IMPORTANT: only one KCDBRegistry object must be in existence for
 * every given file that must be accessed. Multiple processes/classloaders
 * must not attempt to access the same file through this object.
 *
 * Cases that need to be kept in mind when changing this class are:
 * 1. Multiple readers/writers accessing concurrently (users can upload data,
 * the admin GUI can be used to insert data)
 * 2. Other instances of Toxygates accessing the same files concurrently,
 * but from the same JVM (e.g. /adjuvant). Note that such instances can have their own classloaders
 * and, potentially, other versions of this class.
 * 3. Other JVMs accessing the same files concurrently
 * (not currently supported/considered)
 */
object KCDBRegistry {

  private var readCount = Map[String, Int]()
  private var inWriting = Set[String]()
  private var openDBs = Map[String, DB]()

  //Special mode for maintenance.
  private var maintenance: Boolean = false
  def setMaintenance(maintenance: Boolean) {
    this.maintenance = maintenance
  }

//  def isMaintenance: Boolean = maintenance

  /**
   * Remove kyoto cabinet options from file name
   */
  private def realPath(file: String) = file.split("#")(0)

  def get(file: String, writeMode: Boolean) = {
    if (writeMode) {
      getWriter(file)
    } else {
      getReader(file)
    }
  }

  /**
   * Get a reader. The reader count is increased.
   * Readers should be released with the release() call after use.
   */
  def getReader(file: String): Option[DB] = synchronized {
    println(s"Read request for $file")
    val rp = realPath(file)
    if (maintenance) {
      return getWriter(file)
    }

    incrReadCount(rp)
    if (openDBs.contains(rp)) {
      Some(openDBs(rp))
    } else {
      val d = openRead(file)
      openDBs += rp -> d
      Some(d)
    }
  }

  protected[global] def getReadCount(file: String): Int = {
    readCount.getOrElse(realPath(file), 0)
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
   * Get a writer.
   * The request will block until all current readers
   * are finished. If the DB was opened for reading, it will be
   * closed and reopened for writing.
   * Writers should be released with closeWriters() after use.
   */
  def getWriter(file: String): Option[DB] = {
    println(s"Write request for $file")

    val rp = realPath(file)
    synchronized {
      if (inWriting.contains(rp)) {
        return Some(openDBs(rp))
      }
    }

    var count = 0
    var r: Option[DB] = innerGetWriter(file)
    //sleep at most 20s here, waiting for readers to finish
    while (count < 200 && r == None) {
      Thread.sleep(100)
      count += 1
      r = innerGetWriter(file)
    }
    if (r == None) {
      println("Writer request timed out")
    }
    r
  }

  private def innerGetWriter(file: String): Option[DB] = synchronized {
    val rp = realPath(file)
    val rc = readCount.getOrElse(rp, 0)
    println(s"Read count for $file: $rc (waiting for 0)")
    if (rc == 0) {
      if (openDBs.contains(rp)) {
        openDBs(rp).close()
        openDBs -= rp
      }
      val w = openWrite(file)
      inWriting += rp
      openDBs += rp -> w
      Some(w)
    } else {
      None
    }
  }

  /**
   * Release a previously obtained reader. Writers should be closed with
   * closeWriters.
   */
  def releaseReader(file: String): Unit = synchronized {
    if (maintenance) {
      return
    }

    val rp = realPath(file)
    if (readCount.getOrElse(rp, 0) > 0) {
      readCount += (rp -> (readCount(rp) - 1))
      //note we could close the DB here but we keep it open
      //for future users
    } else {
      System.err.println(s"Warning, incorrect release request - $file was not open for reading")
    }
  }

  def closeWriters(): Unit = {
    closeWriters(false)
  }

  /**
   * This method should be called after writing has been finished
   * to close all writers.
   * @param force if true, we close writers even if readers are also open on the same files.
   */
  def closeWriters(force: Boolean): Unit = {
    while(inWriting.size > 0) {
      tryCloseWriters(force)
      if (inWriting.size > 0) {
        println(s"Trying to close writers (waiting for: $inWriting)")
        Thread.sleep(100)
      }
    }
  }

  private def tryCloseWriters(force: Boolean): Unit = synchronized {
    val all = inWriting
    for (f <- all; if (getReadCount(f) == 0 || force)) {
      openDBs(f).close()
      inWriting -= f
      openDBs -= f
    }
  }

  /**
   * Open a database for reading.
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
   * Open a database for writing.
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
