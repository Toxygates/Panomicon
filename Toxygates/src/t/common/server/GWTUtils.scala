package t.common.server

import t.common.shared.GWTTypes

/**
 * Builders of and conversions to/from GWT serialized objects.
 * Not every implementation of interfaces like java.util.List and java.util.Map is
 * serializable, so we control the specific collection types here.
 */
object GWTUtils {
  import scala.collection.JavaConverters._
  import GWTTypes._

  import java.util.{List => JList}

  /**
   * A list type that is compatible with GWT serialization.
   * Actual types produced may be more specific.
   */
  type GWTList[T] = java.util.List[T]
  
  /**
  * A map type that is compatible with GWT serialization.
  * Actual types produced may be more specific.
  */
  type GWTMap[K,V] = java.util.Map[K,V]
  
  implicit class GWTListWrap[T](x: Seq[T]) {
     def asGWT: GWTList[T] = mkList(x.asJava)
  }

  implicit class GWTIterableWrap[T](x: Iterable[T]) {
     def asGWT: GWTList[T] = x.toSeq.asGWT
  }

  implicit class GWTMapWrap[T, U](x: Map[T, U]) {
     def asGWT: GWTMap[T,U] = {
       val r = new java.util.HashMap[T, U]()
       r.putAll(x.asJava)
       r
     }
  }

}
