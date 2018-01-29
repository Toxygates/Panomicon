/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
  var registry: Map[(String, String), File] = Map()

  def makeNew(prefix: String, suffix: String): File = {
    getExisting(prefix, suffix).foreach { file =>
      file.delete()
      registry -= prefix -> suffix
    }
    val f = File.createTempFile(prefix, suffix)
    println("Created temporary file " + f)
    registry += (prefix, suffix) -> f
    f
  }

  def getExisting(prefix: String, suffix: String): Option[File] = {
    registry.get((prefix, suffix))
  }

  def dropAll() {
    for ((key, file) <- registry) {
      println("Deleting temporary file " + file)
      file.delete()
      registry -= key
    }
  }
}
