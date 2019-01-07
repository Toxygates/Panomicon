import scala.io._
import java.io._

/**
 * Crude import script for RFam, to translate probes based on best name match.
 * Results should be treated with caution.
 * 
 * Arguments: mirbase platform file, RFam GEO platform file, 
 * matrix data file to translate
 */

val mirbase = Source.fromFile(args(0)).getLines
val ids = Map() ++ mirbase.map(_.split("\t")(0)).map(x => x.toLowerCase -> x)

def bestMatches(id: String): Option[Seq[String]] = 
  ids.get(id).orElse(ids.get(s"${id}a")).map(Seq(_)).orElse(    
    Some(Seq(s"$id-5p", s"$id-3p", s"${id}a-3p", s"${id}a-5p").flatMap(ids.get))
  )

val rfamPlatform = Source.fromFile(args(1)).getLines
var mapping = Map[String, Seq[String]]()
var success = 0
var fail = 0
var total = 0
for (l <- rfamPlatform; spl = l.split("\t");
  id = spl(0); mirna = spl(5)) {
  val best = bestMatches("mmu-" + mirna.toLowerCase)
  println(s"$mirna => $best")
  if (best.isEmpty || best.get.isEmpty) { 
    fail += 1
  } else {
    mapping += id -> best.get
    success += 1
  }
  total += best.get.size
}
println(s"Success $success fail $fail total range $total")

val w = new PrintWriter(args(2) + "_rfamTranslate.csv")
val input = Source.fromFile(args(2)).getLines
w.println(input.next)
for (
  l <- input;
  spl = l.split(",",2);
  txns <- mapping.get(spl(0));
  txn <- txns
  ) {
  w.println("\"" + s"$txn" + "\"," + spl(1))
}

w.close()
