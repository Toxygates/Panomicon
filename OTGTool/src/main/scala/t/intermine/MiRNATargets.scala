package t.intermine

import scala.collection.JavaConverters._
import org.intermine.pathquery.PathQuery
import org.intermine.pathquery.Constraints
import t.platform._
import t.platform.mirna._
import scala.io.Source

/**
 * Obtain MiRNA targets from TargetMine.
 * Currently these use MirTarBase as an upstream source.
 *
 * This query is currently too large/slow to use in practice.
 * See MiRNATargets.readTableFromFile below for a practical alternative.
 */
class MiRNATargets(conn: Connector) extends Query(conn) {

  def makeQuery(): PathQuery = {
    val pq = new PathQuery(model)

    val synonymsView = "MiRNA.miRNAInteractions.targetGene.synonyms.value"

    pq.addViews("MiRNA.secondaryIdentifier", "MiRNA.miRNAInteractions.supportType",
      synonymsView)

    //NM is to match RefSeq IDs like NM_000389
    pq.addConstraint(Constraints.contains(synonymsView, "NM"))

    println(s"Intermine query: ${pq.toXml}")
    pq
  }

  /**
   * Obtain miRNA-transcript interactions.
   * Returned triples are: (mirna, support type (e.g. functional MTI), refSeq transcript)
   */
  def results: Iterator[(MiRNA, String, RefSeq)] = {
    //Note: versioned IDs like NM_000389.4 will also be obtained, so we filter duplicates

    queryService.getRowListIterator(makeQuery).asScala.map(row => {
      ((MiRNA(row.get(0).toString), row.get(1).toString, RefSeq(row.get(2).toString)))
    }).filter(!_._3.id.contains("."))
  }

  def makeTable: TargetTable = {
    val builder = new TargetTableBuilder
    var n = 0
    for ((mir, support, ref) <- results) {
      builder.add(mir, ref, 100, "MiRTarBase (via TargetMine)")
      //      Progress check for debugging
      n += 1
      if (n % 10000 == 0) {
        println(n)
      }
    }
    builder.build
  }
}

object MiRNATargets {
  def scoreForSupportType(st: String) = {
    st match {
      case "Functional MTI"            => 3
      case "Functional MTI (Weak)"     => 2
      case "Non-Functional MTI"        => 1
      case "Non-Functional MTI (Weak)" => 0
      case _                           => throw new Exception(s"Unexpected support type '$st' in file")
    }
  }

  /*
   * Expected example lines:
   * MIMAT0001632    mmu-miR-451a    Functional MTI  NM_001253806
	 * MIMAT0000531    mmu-miR-22-3p   Functional MTI  NM_001253806
   * MIMAT0000665    mmu-miR-223-3p  Functional MTI (Weak)   NM_001253806
   */
  def tableFromFile(file: String): TargetTable = {
    val lines = Source.fromFile(file).getLines
    val builder = new TargetTableBuilder

    for {
      l <- lines;
      spl = l.split("\\t");
      if (spl.length >= 4)
    } {
      builder.add(MiRNA(spl(1)), RefSeq(spl(3)), scoreForSupportType(spl(2)),
        "MiRTarBase (via TargetMine)")
    }
    builder.build
  }

  def main(args: Array[String]) {
    val conn = new Connector("targetmine", "http://targetmine.mizuguchilab.org/targetmine/service")
    val op = new MiRNATargets(conn)
    //    val res = op.results
    //    println(res.size + " results")
    //    println((res take 10).toVector)
    println(op.makeTable.size)
  }
}
