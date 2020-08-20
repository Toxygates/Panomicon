/**
 * Script to un-transcribe MirBase mature miRNA sequences back into DNA.  
 */

import scala.io._

/**
 * RNA to DNA bases.
 */
def transcribe(char: Char) = char match {
  case 'U' => 'A'
  case 'A' => 'T'
  case 'G' => 'C'
  case 'C' => 'G'
}

def reverseTranscribe(entry: String) = {
  entry.map(transcribe).reverse
}

/**
 * FASTA format sequences, not wrapped
 */
for {
  entry <- Source.stdin.getLines.grouped(2) 
}
{
   println(entry(0))
   println(reverseTranscribe(entry(1)))
}