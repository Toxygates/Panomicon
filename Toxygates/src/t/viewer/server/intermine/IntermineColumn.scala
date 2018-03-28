package t.viewer.server.intermine;

import scala.collection.JavaConversions._

import org.intermine.pathquery.Constraints
import org.intermine.pathquery.PathQuery

import t.db.DefaultBio
import t.sparql._

//TODO remove sparql dependency by refactoring that package
import t.sparql.secondary.Gene
import t.viewer.server.Platforms
import t.intermine.Query

object TargetmineColumns {
  def connector(mines: Intermines,
      platforms: Platforms) =
        new IntermineConnector(mines.byTitle("TargetMine"), platforms)

  def miRNA(connector: IntermineConnector) =
    new IntermineColumn(connector,
        "Gene.miRNAInteractions.miRNA.secondaryIdentifier",
        Seq("Gene.miRNAInteractions.miRNA.secondaryIdentifier"))
//        "Gene.miRNAInteractions.miRNA.primaryIdentifier",
//        Seq("Gene.miRNAInteractions.miRNA.symbol"))
}

/**
 * An association column that is resolved by querying an Intermine
 * data warehouse.
 * @param idView This view is used to provide identifiers for the resulting objects.
 * @param titleViews These views are concatenated to generate the titles of the
 * resulting objects.
 */
class IntermineColumn(connector: IntermineConnector,
    idView: String, titleViews: Iterable[String]) extends Query(connector) {

  val geneIdView = "Gene.primaryIdentifier"

//  private val ls = connector.getListService(None, None)
//  ls.setAuthentication(token)


  private def makeQuery(): PathQuery = {
    val pq = new PathQuery(model)
    pq.addViews(geneIdView, idView)
    pq.addViews(titleViews.toSeq: _*)
    pq
  }

  private def tryParseInt(ident: String): Option[Int] = {
    try {
      Some(Integer.parseInt(ident))
    } catch {
      case nf: NumberFormatException => None
    }
  }

  /**
   * Resolve the column based on NCBI genes
   */
  def forGenes(genes: Iterable[Gene]): MMap[Gene, DefaultBio] = {
    val useGenes = genes.flatMap(g => tryParseInt(g.identifier)).toSeq.distinct

    val pq = makeQuery
    pq.addConstraint(Constraints.lookup("Gene", useGenes.mkString(","), ""))

    println(s"${queryService.getCount(pq)} results")
    makeMultiMap(
      queryService.getRowListIterator(pq).toSeq.map(row => {
        val id = row.get(2).toString
        //TODO might retrieve and insert the score of the association as well
        val tooltip = Some(s"$id (miRTarBase via TargetMine) experimental: true")
        Gene(row.get(0).toString) ->
        DefaultBio(row.get(1).toString, row.get(2).toString, tooltip)
      })
      )

//    val list = connector.addEntrezList(ls, () => genes.map(_.identifier).toSeq.distinct,
//        None, false, List())
//    list match {
//      case Some(l) =>
//        println(s"Added list as ${l.getName} size ${l.getSize} status ${l.getStatus}")
//        val pq = makeQuery
//        pq.addConstraint(Constraints.in("Gene", l.getName))
//        for (row <- qs.getRowListIterator(pq)) {
//          println(Vector() ++ row)
//        }
//        println(s"${qs.getCount(pq)} results")
//
//        emptyMMap()
//      case None => throw new IntermineException("Failed to add temporary list")
//    }
  }
}
