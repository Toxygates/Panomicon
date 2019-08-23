/**
 * Script to convert probes from other/older platforms (e.g. Rfam) into mirbase based on the mature RNA's DNA sequence.
 * Needs a reference mirbase file to perform the matching.
 *
 * Example usage: 1. reverse transcribe mirbase:
 * zcat mirbase-v21-mature.fa.gz | scala transcribeMirbase.scala > mirbase-v21-mature-dna.fa
 *
 * scala matchMirbase.scala mirbase-v21-mature-dna.fa GPL8225-tbl-1.txt GSE13874-GPL8225_series_matrix.data.csv
 */

import scala.io._
import java.io._

//Example entry:
// >hsa-let-7a-5p MIMAT0000062 Homo sapiens let-7a-5p
// AACTATACAACCTACTACCTCA
def mirnaEntry(lines: Seq[String]): (String, String) = {
  val id = lines(0).split("\\s+")(0).drop(1)
  val data = lines(1)
  (data, id)
}


val mirnaDNA = Source.fromFile(args(0)).getLines.grouped(2).
  map(mirnaEntry).toSeq.groupBy(_._1)

println(s"Loaded ${mirnaDNA.size} miRNA entries from ${args(0)}")

//Look up mirBase entries for the probe having sequence dnaSeq
def bestMatches(dnaSeq: String): Seq[String] = {
  //Exact match
  val exact = mirnaDNA.get(dnaSeq).map(_.map(_._2))
  exact match {
    case Some(e) => e
    case None =>
      //Look up entries where one sequence is fully contained in the other
      val partial = mirnaDNA.filter(mirna =>
        mirna._1.contains(dnaSeq) || dnaSeq.contains(mirna._1))
      if (!partial.isEmpty) {
        println(s"Partial match: $dnaSeq -> ${partial.map(_._1)}")
      }
      partial.toSeq.flatMap(_._2.map(_._2))
  }
}

def complement(dna: String) = dna.map {
  case 'T' => 'A'
  case 'A' => 'T'
  case 'C' => 'G'
  case 'G' => 'C'
  case c => c
}
def reverse(dna: String) = dna.reverse

val other = Source.fromFile(args(1)).getLines
var mapping = Map[String, Seq[String]]()
var success = 0
var fail = 0
var total = 0

//Redefine this function to adjust the matching behaviour
def applyTransforms(dna: String) = reverse(complement(dna))

//This column will be used to find the DNA sequence used for lookup
val probeSeqCol = 5
//This column will be used to find the ID that is to be translated into mirbase
val idCol = 0

for (l <- other; spl = l.split("\t");
     id = spl(idCol); dnaSequence = spl(probeSeqCol)) {
  val candidate = applyTransforms(dnaSequence)

  val best = bestMatches(candidate).toList
  println(s"$id => $best")
  if (best.isEmpty) {
    fail += 1
  } else {
    mapping += (id -> best)
    success += 1
  }
  total += best.size
}
println(s"Success $success fail $fail total range $total")

val w = new PrintWriter(args(2) + "_mirbaseTranslate.csv")
val input = Source.fromFile(args(2)).getLines
w.println(input.next)
for (
  l <- input;
  spl = l.split(",", 2);
  txns <- mapping.get(spl(0).replace("\"", ""));
  txn <- txns
) {
  w.println("\"" + s"$txn" + "\"," + spl(1))
}

w.close()