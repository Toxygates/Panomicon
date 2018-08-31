package t.common.server

/**
 * Conversions to/from GWT serialized objects.
 * Not every implementation of interfaces like java.util.List and java.util.Map is
 * serializable, so we control the specific collection types here.
 */
object GWTUtils {
  import scala.collection.JavaConverters._

  implicit class GWTList[T](x: Seq[T]) {
     def asGWT = new java.util.ArrayList(x.asJava)
  }

  implicit class GWTIterable[T](x: Iterable[T]) {
     def asGWT = x.toSeq.asGWT
  }

  implicit class GWTMap[T, U](x: Map[T, U]) {
     def asGWT = {
       val r = new java.util.HashMap[T, U]()
       r.putAll(x.asJava)
       r
     }
  }

}
