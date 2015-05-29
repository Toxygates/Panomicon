/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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
  def read(prefix: String = "", name: String): T
}

trait FileReadable[T] extends Readable[T] {

  def listObjects(prefix: String = ".") = listFiles(prefix)

  def listFiles(prefix: String = "."): Iterable[String] = {
    val f = new File(prefix)
    f.list().toSeq
  }
}