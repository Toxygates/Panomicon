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

import java.io._

if (args.size < 5) {
	println("Syntax: <input file> <output RDF prefix> <rdfs:type (or none)> <identifier prefix> <identifier column (starting from 1, or -1 to autogenerate)> <predicate 1> <predicate 2> ...")
	exit(1)
}

val source = new scala.io.BufferedSource(new FileInputStream(args(0)))
val outPrefix = args(1)
val idColumn = args(4).toInt - 1
val idPrefix = args(3)
val typ = args(2)
val predicates = args.drop(5)

var nextId = 1

for (l <- source.getLines) {
	val items = l.split("\t", -1)
	if (idColumn == -1 || items(idColumn) != "") {
		if (idColumn != -1) {
			println("<" + outPrefix + idPrefix + items(idColumn) + ">")
		} else {
			println("<" + outPrefix + idPrefix + nextId + ">")
			nextId += 1
		}
		if (typ != "none") {
			println("\ta " + typ + ";")
		}
		val triples = (0 until items.size).flatMap( i => {
			if (i != idColumn &&
			i < predicates.size && predicates(i) != "none") {
 			  items(i).split("///").map(p => 
			   "\t<" + outPrefix + predicates(i) +"> \"" + p.trim + "\""
			  )
   			} else {
			   None
 			}
			})
		println(triples.mkString(";\n") + ".")
	}
}
