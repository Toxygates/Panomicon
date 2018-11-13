package t.intermine

import java.io.PrintWriter

import scala.collection.JavaConverters._
import scala.io.Source

import org.intermine.pathquery.Constraints
import org.intermine.pathquery.PathQuery

import t.platform.Species.Species

class EnsemblConversion(conn: Connector, sp: Species) extends Query(conn) {

  def makeQuery: PathQuery = {    
    val pq = new PathQuery(model)
    val synonymView = "Gene.synonyms.value"
    pq.addViews("Gene.ncbiGeneId", synonymView)
    pq.addConstraint(Constraints.contains(synonymView, "ENS"))
    pq.addConstraint(Constraints.equalsExactly("Gene.organism.shortName", sp.shortName))

    println(s"Intermine query: ${pq.toXml}")
    pq
  }

  /**
   * Returns: (NCBI id, ENSEMBL id)
   */
  def results: Iterator[(String, String)] = {
    queryService.getRowListIterator(makeQuery).asScala.map(row => {
      (row.get(0).toString, row.get(1).toString)
    })
  }

  def ensemblToNCBIMap = {
    val pairs = results.toSeq.map(_.swap)
    pairs.groupBy(_._1).mapValues(_.map(_._2))
  }
}

case class GEOPlatformProbe(id: String, ensembl: String, symbol: Seq[String], refseq: Seq[String],
  title: Seq[String]) {

  def asPlatformLine(sp: Species, ensLookup: Map[String, Seq[String]]) = {
    val entrez = ensLookup.getOrElse(ensembl, Seq()).map(ent => s"entrez=$ent")
    
    val items = Seq(
      s"ensembl=$ensembl") ++ 
      Seq(symbol.map("symbol=" + _),
        refseq.map("refseqTrn=" + _),
        title.map("title=" + _),
        entrez).flatten        
    
    s"$id\tspecies=${sp.longName},${items.mkString(",")}"
  }
}

/**
 * Import a platform in GEO format, such as GPL5462 (Toray Oligo 3D-chip for mouse),
 * while also using an Intermine warehouse (Targetmine) to convert ENSEMBL gene IDs
 * into the necessary Entrez that Toxygates needs.
 */
object GEOPlatform {
  /**
   * Arguments: input platform file, species
   */
  def main(args: Array[String]) {
     val conn = new Connector("targetmine", "https://targetmine.mizuguchilab.org/targetmine/service")
     val input = Source.fromFile(args(0)).getLines
     val outFile = args(0) + ".t_platform.tsv"
     val output = new PrintWriter(outFile)
    try {
      val sp = t.platform.Species.withName(args(1))
      val conversion = new EnsemblConversion(conn, sp)
      val ensLookup = conversion.ensemblToNCBIMap
      for (l <- input) {
        output.println(processLine(l).asPlatformLine(sp, ensLookup))
      }
    } finally {
      output.close
    }
  }
  
  def removeCommas(data: String) = data.replace(",", ";")
  
  def asSeq(data: String) = data.trim.split(",").toSeq

  def processLine(line: String) = {
    //Example line:
     //M200000488      ENSMUSG00000020383      Il13    NM_008355       interleukin 13 [Source:MarkerSymbol;Acc:MGI:96541]

    val spl = line.split("\t")
    val probeId = spl(0)
    val ensembl = spl(1)
    val symbol = asSeq(spl(2))
    val refseqTranscript = asSeq(spl(3))
    val title = Seq(removeCommas(spl(4)))

    GEOPlatformProbe(probeId, ensembl, symbol, refseqTranscript, title)    
  }
}
