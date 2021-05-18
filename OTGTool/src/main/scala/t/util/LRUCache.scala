package t.util

/**
 * A key-value cache that evicts the least recently used elements.
 * @param maxElements Number of items to hold
 * @tparam Key Key type
 * @tparam Value Value type
 */
class LRUCache[Key, Value](maxElements: Int) {

  assert(maxElements >= 1)

  var items = List[(Key, Value)]()

  def get(key: Key): Option[Value] = synchronized {
    for (((k, v), i) <- items.zipWithIndex) {
      if (k == key) {
        val before = items take i
        val after = items drop (i + 1)
        items = ((k, v)) :: (before ::: after)
        return Some(v)
      }
    }
    None
  }

  def insert(key: Key, value: Value): Unit = synchronized {
    for (((k, v), i) <- items.zipWithIndex) {
      if (k == key) {
        val before = items take i
        val after = items drop (i + 1)
        items = ((k, value)) :: (before ::: after)
        return
      }
    }
    //Insert the value first and shift the others back
    items = ((key, value)) :: (items take (maxElements - 1))
  }
}
