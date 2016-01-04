/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package t.sparql

import java.io._

import t.TriplestoreConfig
import t.db.DefaultBio
import t.db.ProbeMap
import t.platform.OrthologGroup
import t.platform.OrthologMapping
import t.platform.Probe
import t.platform.ProbeRecord
import t.platform.SimpleProbe
import t.sparql.secondary.GOTerm
import t.sparql.secondary.Gene
import t.sparql.secondary.Protein
import t.util.TempFiles

object Probes extends RDFClass {
  val defaultPrefix: String = s"$tRoot/probe"
  val itemClass: String = "t:probe"
  val hasProbeRelation = "t:hasProbe"

  def recordsToTTL(tempFiles: TempFiles, platformName: String,
    records: Iterable[ProbeRecord]): File = {
    val f = tempFiles.makeNew("probes", "ttl")
    val fout = new BufferedWriter(new FileWriter(f))

    val platform = s"<${Platforms.defaultPrefix}/$platformName>"

    try {
      fout.write(s"@prefix t:<$tRoot/>. \n")
      fout.write("@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>. \n")
      for (p <- records) {
        val probe = s"<$defaultPrefix/${p.id}>"
        fout.write(s"$probe a $itemClass ; rdfs:label " + "\"" + p.id + "\" \n")
        if (p.annotations.size > 0) {
          fout.write("; " + annotationRDF(p))
        }
        fout.write(". \n")
      }
    } finally {
      fout.close()
    }
    f
  }

  def annotationRDF(record: ProbeRecord): String = {
    record.annotations.map(a => {
      val k = a._1
      a._2.map(v => s"t:$k ${ProbeRecord.asRdfTerm(k, v)}").mkString("; ")
    }).mkString(";\n ")
  }

  var _platformsAndProbes: Map[String, Iterable[String]] = null

  //This lookup takes time, so we keep it here as a static resource
  def platformsAndProbes(p: Probes): Map[String, Iterable[String]] = synchronized {
    if (_platformsAndProbes == null) {
      _platformsAndProbes = p.platformsAndProbesLookup
    }
    _platformsAndProbes
  }
}

class Probes(config: TriplestoreConfig) extends ListManager(config) {
  import Triplestore._
  import QueryUtils._
  import Probes._

  def defaultPrefix: String = Probes.defaultPrefix
  def itemClass = Probes.itemClass

  val orthologClass = "t:orthologGroup"

  /**
   * Obtain all ortholog groups in the given platforms.
   * TODO!
   */
  def orthologs(platforms: Iterable[String]): Iterable[OrthologGroup] = {
    val q = tPrefixes +
      "select ?probe ?platform ?title where {" +
      s" ?probe a $itemClass. ?platform $hasProbeRelation ?probe. " +
      s" ?g a $orthologClass ; $hasProbeRelation ?probe. ; rdfs:label ?title . } " +
      multiFilter("?platform", platforms)

    val r = ts.mapQuery(q)
    val byTitle = r.groupBy(_("title"))
    for (
      (title, entries) <- byTitle;
      probes = entries.map(x => SimpleProbe(x("probe"), x("platform")))
    ) yield OrthologGroup(title, probes)
  }

  def orthologMappings: Iterable[OrthologMapping] = {
    val msq = ts.simpleQuery(tPrefixes +
      "SELECT DISTINCT ?om WHERE { ?om a t:ortholog_mapping. }")
    msq.map(m => {
      val mq = ts.mapQuery(s"$tPrefixes select * { graph <$m> { ?x t:hasOrtholog ?o } }")
      val rs = mq.groupBy(_("x")).map(_._2.map(_("o")))
      println(s"Ortholog mapping $m size ${rs.size}")
      OrthologMapping(m, rs.map(_.map(p => Probe.unpack(p).identifier)))
    })
  }

  def forPlatform(platformName: String): Iterable[String] = {
    val platform = s"<${Platforms.defaultPrefix}/$platformName>"
    ts.simpleQuery(tPrefixes + s"select ?l where { graph $platform { " +
      s"?p a ${Probes.itemClass}; rdfs:label ?l . } } ")
  }

  def platformsAndProbes: Map[String, Iterable[String]] =
    Probes.platformsAndProbes(this)

  /**
   * Read all platforms. Costly.
   */
  private def platformsAndProbesLookup: Map[String, Iterable[String]] = {
    val query = tPrefixes + "SELECT DISTINCT ?gl ?pl WHERE { GRAPH ?g { " +
      "?p a t:probe; rdfs:label ?pl. } . ?g rdfs:label ?gl . }"
    val r = ts.mapQuery(query)(30000).map(x => (x("gl"), x("pl"))).groupBy(_._1)
    Map() ++ r.map(x => x._1 -> x._2.map(_._2))
  }

  def numProbes(): Map[String, Int] = {
    val r = ts.mapQuery(s"$tPrefixes select (count(distinct ?p) as ?n) ?l where " +
      s"{ ?pl a ${Platforms.itemClass} ; rdfs:label ?l . graph ?pl {  " +
      s"?p a t:probe. } } group by ?l")
    if (r(0).keySet.contains("l")) {
      Map() ++ r.map(x => x("l") -> x("n").toInt)
    } else {
      // no records
      Map()
    }
  }

  def probeQuery(identifiers: Iterable[String], relation: String,
    timeout: Int = 10000): Query[Seq[String]] =
    Query(tPrefixes +
      "SELECT DISTINCT ?p WHERE { GRAPH ?g { " +
      "?p a t:probe . ",
      s"?p $relation ?filt. " +
        multiFilter("?filt", identifiers),
      //multiUnionObj("?p ", relation, identifiers),
      " } } ",
      eval = ts.simpleQuery(_)(timeout))

  protected def emptyCheck[T, U](in: Iterable[T])(f: => Iterable[U]): Iterable[U] = {
    if (in.isEmpty) {
      Iterable.empty
    } else {
      f
    }
  }

  protected def simpleMapQueryNoEmptyCheck(query: String): MMap[Probe, DefaultBio] = {
    val r = ts.mapQuery(query).map(x => Probe.unpack(x("probe")) ->
      DefaultBio(x("result"), x("result")))
    makeMultiMap(r)
  }

  protected def simpleMapQuery(probes: Iterable[Probe],
    query: String): MMap[Probe, DefaultBio] = {
    if (probes.isEmpty) {
      return emptyMMap()
    }
    simpleMapQueryNoEmptyCheck(query)
  }

  protected class GradualProbeResolver(idents: Iterable[String]) {
    private var remaining = idents.map(_.trim).toSet
    private var result = Set[String]()

    def resolve(prs: Iterable[String]) {
      remaining --= prs
      result ++= prs
    }

    def all: Set[String] = result
    def unresolved: Iterable[String] = remaining.toSeq
  }

  protected def quickProbeResolution(rs: GradualProbeResolver,
    precise: Boolean): Unit = {

  }

  protected def slowProbeResolution(rs: GradualProbeResolver,
    precise: Boolean): Unit = {
    rs.resolve(forGenes(rs.unresolved.map(Gene(_))).map(_.identifier))
  }

  /**
   * Convert a list of identifiers, such as proteins, genes and probes,
   * to a list of probes.
   * final GeneOracle oracle = new GeneOracle();
   */
  def identifiersToProbes(map: ProbeMap, idents: Array[String], precise: Boolean,
    quick: Boolean = false, tritigate: Boolean = false): Iterable[Probe] = {

    val resolver = new GradualProbeResolver(idents)
    //some may be probes already
    resolver.resolve(idents.filter(map.isToken))

    quickProbeResolution(resolver, precise)
    if (!quick) {
      slowProbeResolution(resolver, precise)
    }

    //final filter to remove irrelevant probes
    (resolver.all.filter(map.isToken).toSeq).map(Probe(_))
  }

  private[sparql] def probeToGene: (String, String) = {
    val q = "GRAPH ?probeGraph { ?p a t:probe; t:entrez ?gene }"
    (tPrefixes, q)
  }

  import t.sparql.secondary.B2RKegg
  //TODO best location for this?
  def forPathway(kegg: B2RKegg, pw: t.sparql.secondary.Pathway): Iterable[Probe] = {
    val (p1, q1) = probeToGene
    val (p2, q2) = kegg.attributes(pw)
    val q = s"$p2\n SELECT DISTINCT ?p {\n $q2 \n $q1\n }"
    ts.simpleQuery(q).map(Probe.unpack)
  }

  /**
   * based on a set of entrez gene ID's (numbers), return the
   * corresponding probes.
   */
  def forGenes(genes: Iterable[Gene]): Iterable[Probe] =
    emptyCheck(genes) {
      //TODO: grouped(100) necessary?
      genes.grouped(100).flatMap(g => {
        probeQuery(g.map("\"" + _.identifier + "\""), "t:entrez")()
      }).map(Probe.unpack).toSeq
    }

  /**
   * Based on a set of uniprot protein IDs, return the corresponding
   * probes.
   */
  def forUniprots(uniprots: Iterable[Protein]): Iterable[Probe] =
    emptyCheck(uniprots) {
      probeQuery(uniprots.map("\"" + _.identifier + "\""), "t:swissprot", 20000)()
        .map(Probe.unpack)
    }

  /**
   * For a given sets of proteins, return only those that have some corresponding probe
   * in our dataset.
   */
  def proteinsWithProbes(uniprots: Iterable[Protein]): Iterable[Protein] =
    emptyCheck(uniprots) {
      val query = tPrefixes + "SELECT ?prot WHERE { GRAPH ?g { " + multiUnionObj("?pr", "t:swissprot",
        uniprots.map(p => "\"" + p.identifier + "\". BIND(\"" + p.identifier + "\" AS ?prot)").toSet) + " } }"
      ts.simpleQuery(query).map(Protein(_))
    }

  //TODO
  def allGeneIds(): MMap[Probe, Gene] = emptyMMap()

  def forTitlePatterns(patterns: Iterable[String]): Iterable[Probe] = {
    val query = tPrefixes +
      "SELECT DISTINCT ?p WHERE { GRAPH ?g { " +
      "?p a t:probe ; " +
      "rdfs:label ?l . " +
      caseInsensitiveMultiFilter("?l", patterns.map("\"" + _ + "\"")) +
      " } } "
    ts.simpleQuery(query).map(Probe.unpack)
  }

  /*
   * TODO: consider avoiding the special treatment that attributes like swissprot
   * gets. Such attributes and related sparql queries should be centralised in one
   * column definition.
   */
  def withAttributes(probes: Iterable[Probe]): Iterable[Probe] = {
    def obtain(m: Map[String, String], key: String) = m.getOrElse(key, "")

    val q = tPrefixes +
      """
       SELECT * WHERE { GRAPH ?g {
       ?pr rdfs:label ?l .
       optional { ?pr t:title ?title. }
       optional { ?pr t:swissprot ?prot. }
       ?pr a t:probe . """ +
      multiFilter("?pr", probes.map(p => bracket(p.pack))) + " } ?g rdfs:label ?plat} "
    val r = ts.mapQuery(q)(20000)

    r.groupBy(_("pr")).map(_._2).map(g => {
      val p = Probe(g(0)("l"))
      //TODO!
      val tritiTitle = (g.map(_.get("gene")) ++
        g.map(_.get("protid"))).flatten.toSet.mkString(", ")

      p.copy(
        proteins = g.map(p => Protein(obtain(p, "prot"))).toSet,
        titles = g.map(obtain(_, "title")).toSet, //NB not used
        name = obtain(g.head, "title") + tritiTitle,
        platform = obtain(g.head, "plat"))
    })

  }

  def probesForPartialSymbol(platform: Option[String], title: String): Vector[Probe] = {
    Vector() //TODO
  }

  def goTerms(pattern: String): Iterable[GOTerm] = {
    List() // TODO
  }

  def goTerms(pattern: String, maxSize: Int): Iterable[GOTerm] = {
    List() // TODO
  }

  //TODO this query is inelegant and probably will not work on Fuseki.
  //A better solution is to have the URI of the GOTerm as a starting point to find the
  //probes.
  def forGoTerm(term: GOTerm): Iterable[Probe] = {
    val query = tPrefixes +
    s"""
    PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>

    SELECT DISTINCT ?probe WHERE {
        { ?got go:synonym "${term.name}" . }
        UNION { ?got go:name "${term.name}" . }

         ?probe a t:probe .
        { ?probe t:gomf ?got . }
        UNION { ?probe t:gocc ?got . }
        UNION { ?probe t:gobp ?got . }

    }
    """
    ts.simpleQuery(query).map(Probe.unpack)
  }

  protected def unpackGoterm(term: String) = term.split(".org/")(1)

  def goTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
    goTerms("?probe t:go ?got . ", probes)
  }

  /**
   * find GOterms for given probes.
   * Use go:name instead of go:synonym to ensure we get a single name back.
   */
  protected def goTerms(constraint: String, probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
    val query = tPrefixes +
      """ PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>
      SELECT DISTINCT ?got ?gotname ?probe WHERE { GRAPH ?g {
      ?probe a t:probe . """ +
      constraint +
      multiFilter("?probe", probes.map(p => bracket(p.pack))) +
      """
    }
    { GRAPH ?g2 {
        ?got2 owl:sameAs ?got.
      } GRAPH ?g3 {
        ?got2 go:name ?gotname.
      }
    } UNION {
      ?got go:name ?gotname }
    } """

    val r = ts.mapQuery(query).map(x => Probe.unpack(x("probe")) -> GOTerm(unpackGoterm(x("got")), x("gotname")))
    makeMultiMap(r)
  }

  def probeLists(instanceURI: Option[String]): MMap[String, Probe] = {
    val q = tPrefixes +
      "SELECT DISTINCT ?list ?probeLabel WHERE { GRAPH ?g1 { " +
      instanceURI.map(u =>
        s"?g a ${ProbeLists.itemClass}; ${Instances.memberRelation} <$u>. ").getOrElse("") +
      s"?g ${ProbeLists.memberRelation} ?probeLabel; rdfs:label ?list. } " +
      //TODO: think about this. Handling of RDF1.0/1.1 strings (untyped/typed)
      // "?probe a t:probe; rdfs:label ?probeLabel. " + //filter out invalid probeLabels
      "}"

   //May be slow
    val mq = ts.mapQuery(q)(20000)
    makeMultiMap(mq.map(x => x("list") -> Probe(x("probeLabel"))))
  }

  /**
   * Look up the auxiliary sort map for a given association, identified by
   * a string. TODO: is this the best place for this?
   */
  def auxSortMap(probes: Iterable[String], key: String): Map[String, Double] = {
    Map() ++ probes.map(x => x -> 0.0) //default map does nothing
  }

  def annotationsAndComments: Iterable[(String, String)] = {
    val q = tPrefixes +
    """SELECT DISTINCT ?title ?comment WHERE { ?x a t:annotation;
      rdfs:label ?title; t:comment ?comment } """
    ts.mapQuery(q).map(x => (x("title"), x("comment")))
  }
}
