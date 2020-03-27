/**
 * Part of the Friedrich bioinformatics framework.
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */
package friedrich.util.formats

import java.io.File

/**
 * A trait that represents the ability to read objects of a given type
 * from some form of long term storage.
 */
trait Readable[+T] {

  /**
   * Obtain a list of all objects that can be read.
   */
  def listObjects(prefix: String = ""): Iterable[String]

  /**
   * Read the object with the given name.
   */
//  protected def read(prefix: String = "", name: String): T
}

trait FileReadable[T] extends Readable[T] {

  def listObjects(prefix: String = ".") = listFiles(prefix)

  def listFiles(prefix: String = "."): Iterable[String] = {
    val f = new File(prefix)
    f.list().toSeq
  }
}
