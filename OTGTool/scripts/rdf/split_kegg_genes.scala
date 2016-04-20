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
