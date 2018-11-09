package t.intermine

import scala.io.Source

class EnsemblConversion(conn: Connector) extends Query(conn) {
  
  def results: Iterator[(String, String)] = ???
}

case class GEOPlatformProbe(id: String, ensembl: String, symbol: String, refseq: String,
  title: String) {
  
}

/**
 * Import a platform in GEO format, such as GPL5462 (Toray Oligo 3D-chip for mouse),
 * while also using an Intermine warehouse (Targetmine) to convert ENSEMBL gene IDs
 * into the necessary Entrez that Toxygates needs.
 */
object GEOPlatform {
  /**
   * Arguments: input platform file
   * The resulting platform will be written to standard output.
   */
  def main(args: Array[String]) {
     val conn = new Connector("targetmine", "http://targetmine.mizuguchilab.org/targetmine/service")
     val input = Source.fromFile(args(0)).getLines
     
     
  }
  
  def processLine(line: String) = {
    //Example line:
     //M200000488      ENSMUSG00000020383      Il13    NM_008355       interleukin 13 [Source:MarkerSymbol;Acc:MGI:96541]   
     
    val spl = line.split("\t")
    val probeId = spl(0)
    val ensembl = spl(1)
    val symbol = spl(2)
    val refseqTranscript = spl(3) 
    val title = spl(4)
    
    GEOPlatformProbe(probeId, ensembl, symbol, refseqTranscript, title)
  }
}