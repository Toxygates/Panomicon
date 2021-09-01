/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.server.common

import t.shared.common.GWTTypes

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
