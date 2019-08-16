/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otg.sparql

import scala.collection.JavaConversions._

import t.platform.Species._
import t.TriplestoreConfig
import t.db._
import t.platform.Probe
import t.sparql._
import t.sparql.secondary._
import t.sparql.secondary.B2RKegg

// Note: some code from here could be lifted up
class OTGProbes(config: TriplestoreConfig) extends t.sparql.Probes(config) with Store[Probe] {
  import Probes._

  val prefixes = s"$commonPrefixes PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>"

  def proteins(pr: Probe): Iterable[Protein] = withAttributes(List(pr)).flatMap(_.proteins)
  def genes(pr: Probe): Iterable[Gene] = withAttributes(List(pr)).flatMap(_.genes)

  override def allGeneIds(): MMap[Probe, Gene] = {
    val query = s"""$prefixes
      |SELECT DISTINCT ?p ?x WHERE {
      |  GRAPH ?g {
      |    ?p a t:probe ; t:entrez ?x .
      |  }
      |}""".stripMargin
     makeMultiMap(triplestore.mapQuery(query).map(x => (Probe.unpack(x("p")), Gene(x("x")))))
  }

  override def withAttributes(probes: Iterable[Probe]): Iterable[Probe] = {
    def obtainMany(m: Iterable[Map[String, String]], key: String) = {
      val r = m.filter(_.get("relation") == Some(key)).map(_("value"))
      r.toSeq.distinct
    }

    def obtain(m: Map[String, String], key: String) =
      m.getOrElse(key, "")

    val entrezRel = "http://level-five.jp/t/entrez"
    val symbolRel = "http://level-five.jp/t/symbol"
    val protRel = "http://level-five.jp/t/swissprot"
    val titleRel = "http://level-five.jp/t/title"

	  val q = s"""$prefixes
	    |SELECT * WHERE {
	    |  GRAPH ?g {
	    |    ${valuesMultiFilter("?pr", probes.map(p => bracket(p.pack)))}
      |    OPTIONAL {
      |      ?pr ?relation ?value.
      |      VALUES ?relation { <$entrezRel> <$symbolRel> <$protRel> <$titleRel> }
      |    }
      |    ?pr rdfs:label ?l; a t:probe.
      |  }
      |  ?g rdfs:label ?plat.
      |} """.stripMargin
	  val r = triplestore.mapQuery(q, 20000)

    r.groupBy(_("pr")).map(_._2).map(g => {
      val ident = g.head("l")
      val platform = obtain(g.head, "plat")
      Probe(identifier = ident,
        proteins = obtainMany(g, protRel).map(x => Protein(x)),
        genes = obtainMany(g, entrezRel).map(x =>
          Gene(x,
            keggShortCode = B2RKegg.platformTaxon(platform))),
        symbols = obtainMany(g, symbolRel),
        titles = obtainMany(g, titleRel), //NB not used
        name = obtainMany(g, titleRel).headOption.getOrElse(""),
        platform = platform)
    })
  }

  /**
   * Look up probes by the start of their symbol or ID string. Case-insensitive.
   */
  override def probesForPartialSymbol(platform: Option[String], title: String): Vector[(String, String)] = {
    val query = s"""$prefixes
      |SELECT DISTINCT ?s ?l WHERE {
      |  GRAPH ?g {
      |    ?p a $itemClass; rdfs:label ?l.
      |    OPTIONAL { ?p t:symbol ?s. }
      |  }
      |  ${platform.map(x => "?g rdfs:label \"" + x + "\".").getOrElse("")}
      |  FILTER (
      |   REGEX(STR(?s), "^$title.*", "i") || REGEX(STR(?l), "^$title.*", "i")
      |  )
      |
      |}
      |LIMIT 10""".stripMargin
    triplestore.mapQuery(query).map(x =>
      (x.getOrElse("s", x("l")), x("l")))
  }

  /**
   * Based on a set of gene symbols, return the corresponding probes.
   */
  def forGeneSyms(symbols: Iterable[String], precise: Boolean): MMap[String, Probe] = {
    //Future: we might want to constrain the results by platform.
    val query = s"""$prefixes
      |SELECT DISTINCT ?p ?gene WHERE {
      |  GRAPH ?g {
      |    ?p a t:probe ; t:symbol ?gene .
      |    ${caseInsensitiveMultiFilter("?gene",
             if (precise) symbols.map("\"^" + _ + "$\"") else symbols.map("\"" + _ + "\"")
           )}
      |  }
      |  ?g rdfs:label ?plat }""".stripMargin

    val r = triplestore.mapQuery(query).map(
      x => (symbols.find(s =>
        s.toLowerCase == x("gene").toLowerCase)
        .getOrElse(null) -> Probe.unpack(x("p"))))
    makeMultiMap(r.filter(_._1 != null))
  }

  def mfGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] =
    goTerms("?probe t:gomf ?got . ", probes)

  def ccGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] =
    goTerms("?probe t:gocc ?got . ", probes)

  def bpGoTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] =
   goTerms("?probe t:gobp ?got . ", probes)

  def simpleRelationQuery(probes: Iterable[Probe], relation: String): MMap[Probe, DefaultBio] = {
    val query = s"""$prefixes
      |SELECT DISTINCT ?probe ?result WHERE {
      |  GRAPH ?g {
      |    ?probe $relation ?result.
      |  }
      |  ${multiFilter("?probe", probes.map(p => bracket(p.pack)))}
      |}""".stripMargin
    simpleMapQuery(probes, query)
  }

  def refseqTrnLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.RefseqTranscript.key)

  def refseqProtLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.RefseqProtein.key)

  def ensemblLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.Ensembl.key)

  def ecLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] = {
    //convert e.g. EC:3.6.3.1 into 3.6.3.1
    simpleRelationQuery(probes, "t:" + t.platform.affy.EC.key).
      mapValues(
        _.map(x => x.copy(
            identifier = x.identifier.replace("EC:", ""),
            name = x.identifier.replace("EC:", "")
            ))
        )
  }

  def mirnaAccessionLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:accession")

  def unigeneLookup(probes: Iterable[Probe]): MMap[Probe, DefaultBio] =
    simpleRelationQuery(probes, "t:" + t.platform.affy.Unigene.key)

  override protected def quickProbeResolution(rs: GradualProbeResolver, precise: Boolean): Unit = {
    if (rs.unresolved.size > 0) {
      val geneSyms = forGeneSyms(rs.unresolved, precise).allValues
      rs.resolve(geneSyms.map(_.identifier))
    }
  }

  override protected def slowProbeResolution(rs: GradualProbeResolver, precise: Boolean): Unit = {
    val ups = forUniprots(rs.unresolved.map(p => Protein(p.toUpperCase))).map(_.identifier)
    rs.resolve(ups)
  }
}
