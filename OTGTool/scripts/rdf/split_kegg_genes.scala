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

import scala.io._

//example:
// scala ../split_kegg_genes.scala  ../T01002.ent mmu
// produces a lot of files!

val in = Source.fromFile(args(0)).getLines
val org = args(1)

def next(in: Iterator[String]) {
  val lines = in.takeWhile(!_.startsWith("///")).toSeq
  val id = lines.head.split("\\s+")(1)
  val file = s"${org}_$id.txt"
  val w = new java.io.PrintWriter(file)
  for (l <- lines) {
    w.println(l)
  }
  w.println("///")
  w.close()
}

while (!in.isEmpty) {
  next(in)
}
