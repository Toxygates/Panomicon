import scala.io._

/*
* Script to expand lines in CSV expression data, in the case of multiple probes per line.
*/

val lines = Source.stdin.getLines
println(lines.next)

def unquote(x: String) = x.replace("\"", "")

//The first column only is allowed to contain commas and must then
//be quoted
def extractColumns(line: String): Seq[String] = {
  if (line.contains("\"")) {
    val s = line.split("\",", 2)
    Seq(s(0)) ++ s(1).split(",")
  } else {
    line.split(",")
  }
}

def expandProbe(probe: String) = probe.split(",")

def expandLine(line: String) = {
  val spl = extractColumns(line)
  expandProbe(unquote(spl(0))).map(p => s"$p,${spl.tail.mkString(",")}")
}

for {
  l <- lines
  el <- expandLine(l)
} {
  println(el)
}