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

import scala.io._

val prefixes = List("HSA", "RNO", "MMU")
def adjust(g: String): String = {
  for (p <- prefixes) {
    val ptn = "kegg:" + p + "_"
    if (g.contains(ptn)) {
      //e.g. <http://bio2rdf.org/kegg:HSA_23171>  to 23171
      val spl = g.split(" ")
      val g2 = "\"" + spl(2).split(ptn)(1).replace(">", "") + "\""
      return (List(spl.head, "<http://level-five.jp/t/entrez>", g2) ++ spl.drop(3)).mkString(" ")
    } 
  }
  return g
}

for (l <- Source.stdin.getLines) {
  //e.g. <http://bio2rdf.org/kegg:K00006> <http://bio2rdf.org/kegg_vocabulary:gene> <http://bio2rdf.org/kegg:HSA_23171>  .

  if (l.contains("kegg_vocabulary:gene")) {
    println(adjust(l))
    println(l)
  } else {
    println(l)
  }
}
