package t.global

import kyotocabinet.DB
import scala.collection.mutable.Map

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
      myWriters.get.get(file)
    }
  }
  
  /**
   * Register a thread-local writer for a file.
   * @param db the writer DB to set. If null, then the mapping
   * will be removed.
   */
  private def setMyWriter(file: String, db: DB) = {
    val m = if (myWriters.get == null) {
      val m = Map[String, DB]()
      myWriters.set(m)
      m
    } else {
      myWriters.get
    }
    if (db == null) {
      m -= file
    } else {
      m += (file -> db)
    }
  }
  
  /**
   * Get a reader. The request is granted if there is
   * no existing or pending writer. The reader count is increased.
   * Readers should be released with the release() call after use.
   */
  def getReader(file: String): Option[DB] = synchronized {
    println(s"Read request for $file")
    if (getMyWriter(file) != None) {
      //this thread is writing
      incrReadCount(file)
      getMyWriter(file)      
    } else if (inWriting.contains(file)) {
      println("Writing in progress - reader denied")
      None
    } else {
      incrReadCount(file)
      if (readers.contains(file)) {
        Some(readers(file))
      } else {
        val d = openRead(file)
    	readers += file -> d
    	Some(d)
      }
    }
  } 
  
  private def incrReadCount(file: String) {
      val oc = readCount.getOrElse(file, 0)
      println(s"Read count: $oc")
      readCount += file -> (oc + 1)
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
    synchronized {
      if (getMyWriter(file) != None) {
        return getMyWriter(file)
      } else if (inWriting.contains(file)) {
        println("Writing by other thread in progress - writer denied")
        return None
      }
    }
    
    var count = 0
    var r: Option[DB] = None
    //sleep at most 20s here
    while (count < 10 && r == None) {
      Thread.sleep(2000)
      count += 1
      r = innerGetWriter(file)
    }
    if (r != None) {      
      setMyWriter(file, r.get)
    } else {
      println("Writer request timed out")
    }
    r          
  }
  
  private def innerGetWriter(file: String): Option[DB] = synchronized {
    val rc = readCount.getOrElse(file, 0)
    val iw = inWriting.contains(file)
    println(s"Read count for $file: $rc (waiting for 0) in writing: $iw (waiting for false)")
    if (rc == 0 && !inWriting.contains(file)) {
      if (readers.contains(file)) {
        readers(file).close()
        readers -= file
      }
      val w = openWrite(file) 
      inWriting += file
      Some(w)
    } else {
      None
    }
  }
  
  /**
   * Release a previously obtained reader or writer.
   */
  def release(file: String): Unit = synchronized {
    if (readCount.getOrElse(file, 0) > 0) {
      readCount += (file -> (readCount(file) - 1))
      //note we could close the DB here but we keep it open
      //for future users
    } else if (inWriting.contains(file)) {
      //This release request must come from the same thread
      assert(getMyWriter(file) != None)    
      getMyWriter(file).get.close()
      inWriting -= file
      setMyWriter(file, null)      
    } else {
      throw new Exception(s"Incorrect release request - $file was not open")
    }
  }
  
  /**
   * Open the database for reading.
   */
  private[this] def openRead(file: String): DB = {
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
  private[this] def openWrite(file: String): DB = {
    println(s"Attempting to open $file for writing")
    val db = new DB()
    if (!db.open(file, DB.OWRITER | DB.OCREATE)) {
      throw new Exception("Unable to open db")
    }
    db
  } 
  
}