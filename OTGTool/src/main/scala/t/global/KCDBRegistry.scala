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
 * The KCDBRegistry tracks open Kyoto Cabinet databases.
 * We used to keep them open as long as possible, but now we eagerly close them
 * (for simplicity, since the former approach added complexity and did not seem to
 * buy much performance).
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

  private var maintenance: Boolean = false
  
  /**
   * If maintenance mode is on, everybody gets the same writer, and they are
   * kept open until closeWriters is called at the end.
   * Useful for unrestricted, simple use, if concurrent use is not a concern.
   * In this mode, calling releaseReader is not necessary.
   */
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
    var r: Option[DB] = tryGetWriter(file)
    //sleep at most 20s here, waiting for other users of the file to finish
    while (count < 200 && r == None) {
      Thread.sleep(100)
      count += 1
      r = tryGetWriter(file)
    }
    if (r == None) {
      println("Writer request timed out")
      throw new Exception("Unable to open DB for writing - timeout")
    }
    r
  }

  private def tryGetWriter(file: String): Option[DB] = synchronized {
    val rp = realPath(file)
    openWrite(file) match {
      case Some(w) =>
        inWriting += rp
        openDBs += rp -> w
        Some(w)
      case None =>
        None
    }
  }

  /**
   * Release a previously obtained reader. Writers should be closed with
   * closeWriters.
   * Note that a reader may previously have been opened as a writer, then
   * obtained through a reader request.
   */
  def releaseReader(file: String): Unit = synchronized {
    if (maintenance) {
      return
    }
    val rp = realPath(file)
    if (readCount.getOrElse(rp, 0) > 0) {
      val newCount = readCount(rp) - 1
      readCount += (rp -> newCount)
      if (newCount == 0) {
        if (!inWriting.contains(rp)) {
          if (openDBs(rp).close()) {
            println(s"Closed $rp")
          } else {
            System.err.println(s"Error: failed to close $rp")
          }
          openDBs -= rp
        } else {
          System.out.println(s"Not closing $rp - still open for writing")
        }
        readCount -= rp
      }
    } else {      
      System.err.println(s"Warning, incorrect release request - $file was not open for reading")
    }
  }

  /**
   * This method should be called after writing has been finished
   * to close all writers.
   * @param force if true, we close writers even if readers are also open on the same files.
   */
  def closeWriters(force: Boolean = false): Unit = {
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
    //Writers may be converted into readers, which is why they should not be
    //closed if readCount on the file is > 0.
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
   * Open a database for writing. Will create it if it does not exist.
   */
  private def openWrite(file: String): Option[DB] = {
    println(s"Attempting to open $file for writing")
    val db = new DB()
    if (!db.open(file, DB.OWRITER | DB.OCREATE)) {
      System.out.println("Unable to open db at the moment...")
      None
    } else {
      Some(db)
    }
  }

}
