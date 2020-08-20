/*
 * Copyright (c) 2012-2010 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
 * 
 * Cases that need to be kept in mind when changing this class are:
 *
 * 1. Multiple readers/writers accessing concurrently (users can upload data,
 * the admin GUI can be used to insert data). Handled by only allowing one outstanding writer per file.
 * 2. Other instances of Toxygates accessing the same files concurrently, 
 * but from the same JVM (e.g. /adjuvant). If this is a possibility, handled
 * by ensuring that a global classloader in the servlet container (e.g. Tomcat) loads this class.
 * Otherwise different webapps could have different instances of it.
 * 3. Other JVMs accessing the same files concurrently 
 * (this case is not currently supported/considered)
 */
object KCDBRegistry {

  /**
   * Maps file path to DB object for open writers
   */
  @volatile
  private var openWriters = Map[String, DB]()

  private var maintenance: Boolean = false

  def isMaintenanceMode: Boolean = maintenance

  /**
   * If maintenance mode is on, everybody gets the same DB handle for a given path,
   * always in write mode, and they are
   * kept open until closeWriters is called at the end.
   * Useful for unrestricted, simple use, if concurrent use is not a concern.
   */
  def setMaintenance(maintenance: Boolean) {
    this.maintenance = maintenance
  }

  /**
   * Remove kyoto cabinet options from file name
   */
  private def onlyFileName(file: String) = file.split("#")(0)

  def get(file: String, writeMode: Boolean): Option[DB] = {
    if (writeMode) {
      getWriter(file)
    } else {
      getReader(file)
    }
  }

  /**
   * Get a reader.
   * This class will not track readers. Users should close them manually after use.
   */
  def getReader(file: String): Option[DB] = synchronized {
    println(s"Read request for $file")
    val rp = onlyFileName(file)
    if (maintenance) {
      getWriter(file)
    } else {
     Some(openRead(file))
    }
  }

  private def tryGetWriter(file: String): Option[DB] = synchronized {
    val onlyFile = onlyFileName(file)
    if (openWriters.contains(onlyFile)) {
      //Only one open writer per file allowed concurrently
      None
    } else {
      openWrite(file) match {
        case Some(w) =>
          openWriters += onlyFile -> w
          Some(w)
        case None =>
          throw new Exception("Failed to open database")
      }
    }
  }

  /**
   * Get a writer.
   * Only one outstanding writer per file is allowed. The request
   * will block until any current writers are closed.
   * Writers should be released after use.
   * Note that not even the same thread may request the same writer twice without
   * first closing the first one.
   */
  def getWriter(file: String): Option[DB] = {
    println(s"Write request for $file")

    val filePath = onlyFileName(file)
    if (openWriters.contains(filePath)) {
      if (maintenance) {
        return Some(openWriters(filePath))
      }
    }

    var w = tryGetWriter(file)
    while(w == None) {
      println(s"Waiting for writer on $file")
      Thread.sleep(1000)
      w = tryGetWriter(file)
    }
    w
  }

  def releaseWriter(path: String): Unit = synchronized {
    val file = onlyFileName(path)
    assert (openWriters.contains(file))
    println(s"Close $file")
    openWriters(file).close()
    openWriters -= file
  }

  /**
   * This method should be called when we shut down to close all writers.
   * Intended for maintenance mode and as a failsafe to guard against data corruption.
   * In normal operation, each writer should be released individually after use.
   */
  def closeWriters(): Unit = synchronized {
    for ((f, db) <- openWriters) {
      releaseWriter(f)
    }
  }

  /**
   * Open a database for reading.
   */
  private def openRead(file: String): DB = {
    println(s"Attempting to open $file for reading")
    val db = new DB()
    if (!db.open(file, DB.OREADER)) {
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
