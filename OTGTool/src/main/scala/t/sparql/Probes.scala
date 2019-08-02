/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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
import t.platform._
import t.sparql.secondary._
import t.util.TempFiles
import t.platform.Species
import t.platform.Species._

object Probes extends RDFClass {
  val defaultPrefix: String = s"$tRoot/probe"
  val itemClass: String = "t:probe"
  val hasProbeRelation = "t:hasProbe"

  def probeAttributePrefix = tRoot

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
          fout.write("; " + p.annotationRDF)
        }
        fout.write(". \n")
      }
    } finally {
      fout.close()
    }
    f
  }

  var _platformsAndProbes: Map[String, Iterable[Probe]] = null

  //This lookup takes time, so we keep it here as a static resource
  def platformsAndProbes(p: Probes): Map[String, Iterable[Probe]] = synchronized {
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

  def orthologMappings: Iterable[OrthologMapping] = {
    val msq = triplestore.simpleQuery(tPrefixes + '\n' +
      "SELECT DISTINCT ?om WHERE { ?om a t:ortholog_mapping. }")
    msq.map(m => {
      val mq = triplestore.mapQuery(s"$tPrefixes SELECT * { GRAPH <$m> { ?x t:hasOrtholog ?o } }")
      val rs = mq.groupBy(_("x")).map(_._2.map(_("o")))
      println(s"Ortholog mapping $m size ${rs.size}")
      OrthologMapping(m, rs.map(_.map(p => Probe.unpack(p).identifier)))
    })
  }

  def forPlatform(platformName: String): Iterable[String] = {
    val platform = s"<${Platforms.defaultPrefix}/$platformName>"
    triplestore.simpleQuery(s"""$tPrefixes
        |SELECT ?l WHERE {
        |  GRAPH $platform {
        |    ?p a ${Probes.itemClass}; rdfs:label ?l .
        |  }
        |}""".stripMargin)
  }

  def platformsAndProbes: Map[String, Iterable[Probe]] =
    Probes.platformsAndProbes(this)

  /**
   * Read all platforms. Slow.
   * Probes will be potentially annotated with
   * entrez, refseq atranscript and symbol information only.
   */
  private def platformsAndProbesLookup: Map[String, Iterable[Probe]] = {
    /*
     * An important concern with these queries is reducing the number of tuples being returned.
     * Doing SELECT DISTINCT on all the variables (e.g. ?ent, ?trn, ?sym)
     * will cause a combinatorial explosion in
     * this case, where one probe can have e.g. multiple gene symbols and transcripts.
     * GROUP_CONCAT aggregates such multiple values so that only one tuple is returned for all
     * such combinations.
     * Another interesting aggregation function that may be used is SAMPLE.
     */

    val query = s"""$tPrefixes
       |SELECT ?gl ?pl
       |  (GROUP_CONCAT(?ent; separator = ":") AS ?entCon)
       |  (GROUP_CONCAT(?trn; separator = ":") AS ?trnCon)
       |  (GROUP_CONCAT(?sym; separator = ":") AS ?symCon) WHERE {
       |  GRAPH ?g {
       |    ?p a t:probe; rdfs:label ?pl.
       |    OPTIONAL {
       |      ?p t:entrez ?ent.
       |      ?p t:refseqTrn ?trn.
       |      ?p t:symbol ?sym.
       |    }
       |   } . ?g rdfs:label ?gl .
       |}
       |GROUP BY ?pl ?gl
       |""".stripMargin

    val r = triplestore.mapQuery(query, 120000)

    val all = for (probe <- r;
      probeId = probe("pl");
      platform = probe("gl");
      entrez = probe.get("entCon").toSeq.flatMap(_.split(":")).map(Gene(_));
      transcripts = probe.get("trnCon").toSeq.flatMap(_.split(":")).map(RefSeq(_));
      symbols = probe.get("symCon").toSeq.flatMap(_.split(":"));
      pr = Probe(probeId, genes = entrez.distinct,
        platform = platform,
        transcripts = transcripts.distinct,
        symbols = symbols.distinct)
    ) yield (platform, pr)

    Map() ++ all.groupBy(_._1).mapValues(_.map(_._2))
  }

  def numProbes(): Map[String, Int] = {
    val r = triplestore.mapQuery(s"""$tPrefixes
        |SELECT (count(distinct ?p) as ?n) ?l WHERE {
        |  ?pl a ${Platforms.itemClass} ; rdfs:label ?l .
        |  GRAPH ?pl {
        |    ?p a t:probe.
        |   }
        | } GROUP BY ?l""".stripMargin)
    if (r(0).keySet.contains("l")) {
      Map() ++ r.map(x => x("l") -> x("n").toInt)
    } else {
      // no records
      Map()
    }
  }

  def probeQuery(identifiers: Iterable[String], relation: String,
    timeout: Int = 10000): Query[Seq[String]] =
    Query(s"""$tPrefixes
        |SELECT DISTINCT ?p WHERE {
        |  GRAPH ?g {
        |   ?p a t:probe.
        |   ?p $relation ?filt.""".stripMargin +
        multiFilter("?filt", identifiers),
      //multiUnionObj("?p ", relation, identifiers),
          "   } ",
      eval = triplestore.simpleQuery(_, false, timeout))

  protected def emptyCheck[T, U](in: Iterable[T])(f: => Iterable[U]): Iterable[U] = {
    if (in.isEmpty) {
      Iterable.empty
    } else {
      f
    }
  }

  protected def simpleMapQueryNoEmptyCheck(query: String): MMap[Probe, DefaultBio] = {
    val r = triplestore.mapQuery(query).map(x => Probe.unpack(x("probe")) ->
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
   */
  def identifiersToProbes(map: ProbeMap, idents: Array[String], precise: Boolean,
    quick: Boolean = false): Iterable[Probe] = {

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

  private[this] def probeToGene: (String, String) = {
    val q = "GRAPH ?probeGraph { ?p a t:probe; kv:x-ncbigene ?gene }"
    (tPrefixes, q)
  }

  import t.sparql.secondary.B2RKegg
  //TODO best location for this?
  //Think about how to combine query fragments from different domains like this
  def forPathway(kegg: B2RKegg, pw: t.sparql.secondary.Pathway): Iterable[Probe] = {
    val (p1, q1) = probeToGene
    val (p2, q2) = kegg.attributes(pw)
    val q = s"$p2\n SELECT DISTINCT ?p {\n $q2 \n $q1\n }"
    triplestore.simpleQuery(q).map(Probe.unpack)
  }

  /**
   * Based on a set of entrez gene ID's (numbers), return the
   * corresponding probes.
   */
  def forGenes(genes: Iterable[Gene]): Iterable[Probe] =
    emptyCheck(genes) {
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
      triplestore.simpleQuery(query).map(Protein(_))
    }

  /**
   * Retrieve all genes. In most cases, subclasses should override this method.
   */
  def allGeneIds(): MMap[Probe, Gene] = emptyMMap()

  def forTitlePatterns(patterns: Iterable[String]): Iterable[Probe] = {
    val query = s"""$tPrefixes
         |SELECT DISTINCT ?p WHERE {
         |  GRAPH ?g {
         |    ?p a t:probe ; rdfs:label ?l . """.stripMargin +
      caseInsensitiveMultiFilter("?l", patterns.map("\"" + _ + "\"")) +
      " } } "
    triplestore.simpleQuery(query).map(Probe.unpack)
  }

  def withAttributes(probes: Iterable[Probe]): Iterable[Probe] = {
  /*
   * While title and platform seem sufficiently general, it's open-ended whether attributes like
   * swissprot (UniProt proteins) should get this kind of special treatment.
   */

    def obtain(m: Map[String, String], key: String) = m.getOrElse(key, "")

    val q = s"""$tPrefixes
         |SELECT * WHERE {
         |  GRAPH ?g {
         |    ?pr rdfs:label ?l .
         |    OPTIONAL { ?pr t:title ?title. }
         |    OPTIONAL { ?pr t:swissprot ?prot. }
         |    ?pr a t:probe . """.stripMargin +
      multiFilter("?pr", probes.map(p => bracket(p.pack))) + " } ?g rdfs:label ?plat } "
    val r = triplestore.mapQuery(q, 20000)

    r.groupBy(_("pr")).map(_._2).map(g => {
      val p = Probe(g(0)("l"))

      p.copy(
        proteins = g.map(p => Protein(obtain(p, "prot"))).toSeq.distinct,
        titles = g.map(obtain(_, "title")).toSeq.distinct, //NB not used
        name = obtain(g.head, "title"),
        platform = obtain(g.head, "plat"))
    })

  }

  def probesForPartialSymbol(platform: Option[String], title: String): Vector[(String, String)] = {
    ???
  }

  /**
   * Find GO terms matching the given string pattern.
   */
  def goTerms(pattern: String): Iterable[GOTerm] = {
    goTerms(pattern, 1000)
  }

  def goTerms(pattern: String, maxSize: Int): Iterable[GOTerm] = {
    //oboInOwl:id is a trick to distinguish GO terms from KEGG pathways, mostly

    val query = s"""$tPrefixes
        |PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
        |SELECT DISTINCT ?got ?gotn WHERE {
        |  GRAPH ?g {
        |    ?got rdfs:label ?gotn ; oboInOwl:id ?id
        |  } """.stripMargin +
    "FILTER regex(STR(?gotn), \".*" + pattern + ".*\", \"i\")" +
    s"} LIMIT ${maxSize}"
    triplestore.mapQuery(query).map(x => GOTerm(unpackGoterm(x("got")), x("gotn")))
  }

  //TODO A better solution is to have the URI of the GOTerm as a starting point to find the
  //probes.
  def forGoTerm(term: GOTerm): Iterable[Probe] = {
    val query = s"""$tPrefixes
        |SELECT DISTINCT ?probe WHERE {
        |  GRAPH ?g {
        |    ?got rdfs:label ?label.
        |  }
        |  FILTER(STR(?label) = "${term.name}"^^xsd:string).
        |  GRAPH ?g2 {
        |    ?probe a t:probe .
        |    { ?probe t:gomf ?got . }
        |      UNION { ?probe t:gocc ?got . }
        |      UNION { ?probe t:gobp ?got . }
        |      UNION { ?probe t:go ?got . }
        |  }
        |}""".stripMargin
    triplestore.simpleQuery(query).map(Probe.unpack)
  }

  protected def unpackGoterm(term: String) = term.split(".org/obo/")(1).replace("_", ":")

  def goTerms(probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
    goTerms("?probe t:go ?got . ", probes)
  }

  /**
   * find GOterms for given probes.
   * Use go:name instead of go:synonym to ensure we get a single name back.
   */
  protected def goTerms(constraint: String, probes: Iterable[Probe]): MMap[Probe, GOTerm] = {
    val query = s"""$tPrefixes
          |PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>
          |SELECT DISTINCT ?got ?gotname ?probe WHERE {
          |  GRAPH ?g {
          |    ?probe a t:probe .
          |    $constraint
          |  }
          |  ${multiFilter("?probe", probes.map(p => bracket(p.pack)))}
          |  GRAPH ?g2 {
          |    ?got rdfs:label ?gotname.
          |  }
          |  FILTER (?got NOT IN (<http://purl.obolibrary.org/obo/GO_0003674>, <http://purl.obolibrary.org/obo/GO_0005575>,
    <http://purl.obolibrary.org/obo/GO_0008150>) )
          |}""".stripMargin

    val r = triplestore.mapQuery(query).map(x => Probe.unpack(x("probe")) -> GOTerm(unpackGoterm(x("got")), x("gotname")))
    makeMultiMap(r)
  }

  def probeLists(instanceURI: Option[String]): MMap[String, Probe] = {
    val q = s"""$tPrefixes
          |SELECT DISTINCT ?list ?probeLabel WHERE {
          |  GRAPH ?g1 {""".stripMargin +
      instanceURI.map(u =>
        s"?g a ${ProbeLists.itemClass}; ${Instances.memberRelation} <$u>. ").getOrElse("") +
      s"?g ${ProbeLists.memberRelation} ?probeLabel; rdfs:label ?list. } " +
      //Note: string matching, such as in the following fragment, is impacted by
      //the handling of RDF1.0/1.1 strings (untyped/typed)
      // "?probe a t:probe; rdfs:label ?probeLabel. " + //filter out invalid probeLabels
      "}"

   //May be slow
    val mq = triplestore.mapQuery(q, 20000)
    makeMultiMap(mq.map(x => x("list") -> Probe(x("probeLabel"))))
  }

  /**
   * Look up the auxiliary sort map for a given association, identified by
   * a string.
   *
   * This mechanism is not currently used. If we revive it, then it might possibly
   * be moved to a different location.
   */
  def auxSortMap(probes: Iterable[String], key: String): Map[String, Double] = {
    Map() ++ probes.map(x => x -> 0.0) //default map does nothing
  }

  def annotationsAndComments: Iterable[(String, String)] = {
    val q = s"""$tPrefixes
       |SELECT DISTINCT ?title ?comment WHERE {
       |  GRAPH ?x {
       |    ?x a t:annotation; rdfs:label ?title; t:comment ?comment
       |  }
       |} """.stripMargin
    triplestore.mapQuery(q).map(x => (x("title"), x("comment")))
  }

  /**
   * MiRNA association sources.
   * Format: (id, name, scores available?, suggested limit, size, comment)
   * For dynamic sources, the ID string is the triplestore graph.
   */
  def mirnaSources: Iterable[(String, String, Boolean, Option[Double], Option[Int],
      Option[String])] = {
    val q = s"""$tPrefixes
      |SELECT DISTINCT * WHERE {
      |  GRAPH ?g {
      |    ?g a t:mirnaSource; rdfs:label ?title; t:hasScores ?hasScores;
      |    OPTIONAL { ?g t:suggestedLimit ?suggestedLimit; t:size ?size; t:comment ?comment. }.
      |  }
      |}""".stripMargin
      triplestore.mapQuery(q).map(x =>
        (x("g"), x("title"), x("hasScores").toBoolean,
            x.get("suggestedLimit").map(_.toDouble), x.get("size").map(_.toInt),
            x.get("comment"))
        )
  }

  private def mirnaQueryGraphs(platform: Option[String], mirnaSourceGraph: String) = {
    val r = platform match {
      case Some(s)   => s"FROM <${Platforms.context(s)}>"
      case _ =>
        Species.knownPlatforms.map(p => s"FROM <${Platforms.context(p)}>").mkString("\n")
    }
    s"""|$r
       |$mirnaSourceGraph""".stripMargin
  }

  private def mirnaToMrnaQuery(queryForFilter: Iterable[String],
      scoreLimit: Option[Double]) =
    s"""|WHERE {
       |  [ t:refseqTrn ?trn; t:mirna ?mirna; t:score ?score ].
       |  ${valuesMultiFilter("?mirna", queryForFilter)}
       |  ${scoreLimit.map(s => s"FILTER(?score > $s)").getOrElse("")}
       |  ?mrna t:refseqTrn ?trn; a t:probe; t:symbol ?symbol.
       |}""".stripMargin

  private def mrnaToMirnaQuery(queryForFilter: Iterable[String],
    scoreLimit: Option[Double]) =
    s"""|WHERE {
       |  ?mrna t:refseqTrn ?trn; a t:probe.
       |  ${valuesMultiFilter("?mrna", queryForFilter)}
       |  [ t:refseqTrn ?trn; t:mirna ?mirna; t:score ?score ].
       |  ${scoreLimit.map(s => s"FILTER(?score > $s)").getOrElse("")}
       |}""".stripMargin

  /**
   * Obtain mRNA-miRNA associations.
   * Note: this is not currently used - target tables are loaded from text files instead.
   * @param queryFromMirna if true, query probes are mirna, otherwise mrna.
   */
  def mirnaAssociations(probes: Iterable[Probe], scoreLimit: Option[Double],
                        queryFromMirna: Boolean,
                        maxCount: Option[Int],
                        mirnaSourceGraph: String,
                        experimental: Boolean,
                        mirnaSourceName: String,
                        platform: Option[String] = None): MMap[Probe, DefaultBio] = {

    val queryVar = if(queryFromMirna) "mirna" else "mrna"
    val targetVar = if(queryFromMirna) "mrna" else "mirna"
    val queryForFilter = probes.map(p => bracket(p.pack))

    if (probes.isEmpty) {
      return emptyMMap()
    }

    val q = s"""$tPrefixes
    |SELECT DISTINCT *
    |${mirnaQueryGraphs(platform, mirnaSourceGraph)}
    |${ if (queryFromMirna)
      mirnaToMrnaQuery(queryForFilter, scoreLimit) else
      mrnaToMirnaQuery(queryForFilter, scoreLimit)}
    |${maxCount.map(c => s"LIMIT $c").getOrElse("")}
    |""".stripMargin

    val r = triplestore.mapQuery(q, 30000).map(x => {
      val score = x("score")
      val refseq = x("trn")
      val query = x(queryVar)
      val target = x(targetVar)

      val extraInfo = s"${Probe.unpackOnly(target)} ($mirnaSourceName) experimental: $experimental score: ${"%.3f".format(score.toDouble)} via: $refseq"

      Probe.unpack(query) ->
        DefaultBio(Probe.unpackOnly(target),
          x.getOrElse("symbol", Probe.unpackOnly(target)),
          Some(extraInfo))
    })
    makeMultiMap(r)
  }
}
