package t.intermine

import scala.io.Source
import org.intermine.pathquery.PathQuery
import org.intermine.pathquery.Constraints
import t.platform.Species.Species
import scala.collection.JavaConverters._
import org.intermine.pathquery.OrderDirection

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

case class GEOPlatformProbe(id: String, ensembl: String, symbol: String, refseq: String,
  title: String) {

  def asPlatformLine(sp: Species, ensLookup: Map[String, Seq[String]]) = {
    val entrez = ensLookup(ensembl).map(ent => s"entrez=$ent").mkString(",")
    s"$id\tspecies=${sp.longName},title=$title,symbol=$symbol,ensembl=$ensembl,refseqTrn=$refseq,entrez=$entrez"
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
   * The resulting platform will be written to standard output.
   */
  def main(args: Array[String]) {
     val conn = new Connector("targetmine", "https://targetmine.mizuguchilab.org/targetmine/service")
     val input = Source.fromFile(args(0)).getLines
     val sp = t.platform.Species.withName(args(1))
     val conversion = new EnsemblConversion(conn, sp)
     val ensLookup = conversion.ensemblToNCBIMap
     for (l <- input) {
       println(processLine(l).asPlatformLine(sp, ensLookup))
     }
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
