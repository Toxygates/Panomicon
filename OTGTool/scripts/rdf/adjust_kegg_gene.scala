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
