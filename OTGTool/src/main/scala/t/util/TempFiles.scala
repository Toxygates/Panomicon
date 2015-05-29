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

package t.util

import java.io.File

/**
 * A way of managing a set of temporary files that can be 
 * released collectively
 */
class TempFiles {
  var registry: Vector[File] = Vector()

  def makeNew(prefix: String, suffix: String): File = {
    val f = File.createTempFile(prefix, suffix)
    println("Created temporary file " + f)
    registry :+= f
    f
  }

  def dropAll() {
    for (f <- registry) {
      println("Deleting temporary file " + f)
      f.delete()
    }
  }
}