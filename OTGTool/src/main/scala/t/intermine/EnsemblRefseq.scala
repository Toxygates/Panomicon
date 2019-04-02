package t.intermine

import org.intermine.pathquery.PathQuery
import t.platform.Species.Species
import org.intermine.pathquery.Constraints
import scala.collection.JavaConverters._
import scala.io.Source


class EnsemblRefseq(conn: Connector, sp: Species) extends Query(conn) {

  def makeQuery(constraint: String): org.intermine.pathquery.PathQuery = {
    val pq: org.intermine.pathquery.PathQuery = new PathQuery(model)

    val synonymsView: String = "Gene.synonyms.value"

    pq.addViews("Gene.primaryIdentifier", synonymsView)

    //NM is to match RefSeq IDs like NM_000389
    pq.addConstraint(Constraints.contains(synonymsView, constraint))
    pq.addConstraint(Constraints.equalsExactly("Gene.organism.shortName", sp.shortName))
    println(s"Intermine query: ${pq.toXml}")
    pq
  }

  def ensemblQuery = makeQuery("ENS")
  def refseqQuery = makeQuery("NM_")

  def asMap(q: PathQuery) = {
    queryService.getRowListIterator(q).asScala.map(row => {
      (row.get(0).toString, row.get(1).toString)
    }).toList.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))
  }

  val ensemblMap = asMap(ensemblQuery)
  val refseqMap = asMap(refseqQuery)

  val ensToRefseq: Map[String, Seq[String]] = (for {
    (id, enss) <- ensemblMap.toSeq
    e <- enss
    r <- refseqMap.getOrElse(id, Seq())
  } yield (e, r)).groupBy(_._1).map(x => (x._1 -> x._2.map(_._2)))
}

/**
 * Application to fetch an Ensembl -> RefSeq conversion table from Intermine.
 */
object EnsemblRefseq {
  def main(args: Array[String]) {
    val conn = new Connector("targetmine", "https://targetmine.mizuguchilab.org/targetmine/service")

    for (s <- t.platform.Species.values) {
      val er = new EnsemblRefseq(conn, s)
      println(er.ensemblMap take 10)
      println(er.refseqMap take 10)
      println(er.ensToRefseq take 10)
    }
  }
}

/**
 * Application to import MiRAW output data, converting Ensembl gene IDs into RefSeq transcripts.
 */
object MiRawImporter {
  def main(args: Array[String]) {
    val lines = Source.fromFile(args(0)).getLines.drop(1)
    val species = t.platform.Species.withName(args(1))
    val conn = new Connector("targetmine", "https://targetmine.mizuguchilab.org/targetmine/service")
    val er = new EnsemblRefseq(conn, species)


    for (l <- lines) {
      val s = l.split("\t", -1)
      val mirbase = s(2)
      val gene = s(1)
      val realClass = s(4)
      val prediction = s(3)

      for (ref <- er.ensToRefseq.getOrElse(gene, Seq())) {
        if (realClass.toInt == 1) {
          println(s"$mirbase\t$ref")
        }
      }
    }
  }
}