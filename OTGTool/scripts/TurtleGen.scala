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
