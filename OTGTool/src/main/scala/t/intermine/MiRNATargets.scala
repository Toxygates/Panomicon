package t.intermine

import scala.collection.JavaConverters._
import org.intermine.pathquery.PathQuery
import org.intermine.pathquery.Constraints
import t.platform._
import t.platform.mirna._

/**
 * Obtain MiRNA targets from TargetMine.
 * Currently these use MirTarBase as an upstream source.
 * 
 * This query is currently too large/slow to use in practice.
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
    }).filter(! _._3.id.contains("."))
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
  def main(args: Array[String]) {
    val conn = new Connector("targetmine", "http://targetmine.mizuguchilab.org/targetmine/service")
    val op = new MiRNATargets(conn)
//    val res = op.results
//    println(res.size + " results")
//    println((res take 10).toVector)
    println(op.makeTable.size)
  }
}