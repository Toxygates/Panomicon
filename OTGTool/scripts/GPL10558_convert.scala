import scala.io._

val in = Source.stdin.getLines

def optValue(x: String) = 
	if (x.trim.length > 0) Some(x) else None


for (x <- in) {
	val xs = x.split("\t", -1)
	if (xs.length >= 14) {
		val attrs = Seq(
			optValue(xs(6)).map("refseqTrn=" + _),
			optValue(xs(7)).map("unigene=" + _),
			optValue(xs(8)).map("entrez=" + _),
			optValue(xs(11)).map("symbol=" + _)
		)
		val id = xs(13)
		print(s"$id\t")
		println(attrs.flatten.mkString(","))
	} else {
		Console.err.println(s"Line too short: $x")
	}
}

