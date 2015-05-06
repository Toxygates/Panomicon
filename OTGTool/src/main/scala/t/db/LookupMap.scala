package t.db

/**
 * A set of string values that can quickly and reversibly be 
 * converted to another type T. Useful for database encodings.
 * 
 * TODO: Do we need this trait? Will it do to use a standard collection Map?
 */
trait LookupMap[T] {
  /**
   * Keys that are actually used
   */
  def keys: Set[T]
  
  def tokens: Set[String]
  
  def pack(item: String): T
  def unpack(item: T): String  
  def tryUnpack(item: T): Option[String]
  
  def isToken(t: String): Boolean = tokens.contains(t)
}

trait CachedLookupMap[T] extends LookupMap[T] {
  def data: Map[String, T]
  
  protected[this] val map = data
  protected[this] val revMap = Map[T, String]() ++ map.map(_.swap)

  def keys = revMap.keySet
  def tokens: Set[String] = map.keySet
  def pack(item: String): T = map.get(item).getOrElse(
      throw new LookupFailedException(s"Lookup failed for $item"))
  def unpack(item: T): String = revMap(item)
  def tryUnpack(item: T) = revMap.get(item)
}

abstract class MemoryLookupMap[T](val data: Map[String, T]) 
	extends CachedLookupMap[T] 

class LookupFailedException(reason: String) extends Exception(reason)

/**
 * A probe encoding.
 */
trait ProbeMap extends LookupMap[Int] 

trait SampleMap extends LookupMap[Int] 

