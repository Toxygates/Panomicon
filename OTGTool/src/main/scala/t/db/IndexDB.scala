package t.db

/**
 * An IndexDB maps human-readable string identifiers (URIs or otherwise) to 
 * integer ID:s that may be used for further lookup in other databases.
 * In addition to its default key space, it also stores named enums.
 * Enum values cannot currently be removed.
 */
trait IndexDB {

  /**
   * Retrieve the entire map encoded by this IndexDB.
   */
  def fullMap: Map[String, Int] 
  
  /**
   * Retrieve all keys.
   */
  def keys: Iterable[String] = fullMap.keySet
  
  /**
   * Retrieve all values.
   */
  def values: Iterable[Int] = fullMap.values
  
  /**
   * Retrieve a mapping.
   */
  def get(key: String): Option[Int] 
  
  /**
   * Retrieve a value in an enum.
   */
  def get(enum: String, key: String): Option[Int]
  
  /**
   * Retrieve a full enum mapping.
   */
  def enumMap(name: String): Map[String, Int]
  
  def enumMaps(keys: Iterable[String]): Map[String, Map[String, Int]] = 
    Map() ++ keys.map(k => (k -> enumMap(k)))
}

/**
 * A writable IndexDB.
 */
trait IndexDBWriter extends IndexDB {  
  /**
   * Store a mapping.
   * Returns the newly generated value.
   */
  def put(key: String): Int
  
  /**
   * Store a mapping in an enum.
   * Returns the newly generated value.
   */
  def put(enum: String, key: String): Int
  
  /**
   * Remove a mapping.
   */
  def remove(key: String): Unit
  
  /**
   * Find a mapping for the given key, or create it if it didn't exist.
   */
  def findOrCreate(key: String): Int = get(key).getOrElse(put(key))
  
  /**
   * Find a mapping in an enum, or create it if it didn't exist.
   */
  def findOrCreate(enum: String, key: String) =
    get(enum, key).getOrElse(put(enum, key))
  
    /**
     * Remove values from the default mapping.
     */
  def remove(keys: Iterable[String]): Unit = {
    for (k <- keys) {
      remove(k)
    }
  } 
}

class SampleIndex(db: Map[String, Int]) extends MemoryLookupMap[Int](db) with SampleMap {
  def this(db: IndexDB) = this(db.fullMap)
}

class ProbeIndex(db: Map[String, Int]) extends MemoryLookupMap(db) with ProbeMap {
   def this(db: IndexDB) = this(db.fullMap)        
}
