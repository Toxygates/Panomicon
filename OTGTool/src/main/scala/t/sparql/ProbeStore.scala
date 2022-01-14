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

package t.sparql

import java.io._

import t.TriplestoreConfig
import t.db.{DefaultBio, ProbeMap, Store}
import t.platform._
import t.sparql.secondary._
import t.util.TempFiles

object ProbeStore extends RDFClass {
  val defaultPrefix: String = s"$tRoot/probe"
  val itemClass: String = "t:probe"
  val hasProbeRelation = "t:hasProbe"

  def probeAttributePrefix = tRoot

  def recordsToTTL(tempFiles: TempFiles, platformName: String,
                   records: Iterable[ProbeRecord]): File = {
    val f = tempFiles.makeNew("probes", "ttl")
    val fout = new BufferedWriter(new FileWriter(f))

    val platform = s"<${PlatformStore.defaultPrefix}/$platformName>"

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

  /*
   * Platforms are gradually loaded and cached here when needed
   */
  private var platformProbeCache: Map[String, Iterable[Probe]] = Map.empty
  private var ppCacheFullyLoaded = false

  def platformsAndProbes(pfs: PlatformStore, prs: ProbeStore): Map[String, Iterable[Probe]] = synchronized {
    if (!ppCacheFullyLoaded) {
      for {
        pf <- pfs.getList();
        if !platformProbeCache.contains(pf)
      } {
        platformProbeCache += (pf -> prs.annotatedProbesForPlatform(pf))
      }
      ppCacheFullyLoaded = true
    }
    platformProbeCache
  }

  def loadOrFetchFromCache(prs: ProbeStore, platform: String): Iterable[Probe] = synchronized {
    if (!platformProbeCache.contains(platform)) {
      platformProbeCache += (platform -> prs.annotatedProbesForPlatform(platform))
    }
    platformProbeCache(platform)
  }
}

trait PlatformLoader {
  /**
   * Load probes for one platform.
   */
  def probesForPlatform(platform: String): Iterable[Probe]

  /**
   * Eagerly load all platforms and probes. Expensive.
   */
  def allPlatforms: Map[String, Iterable[Probe]]
}

class ProbeStore(val config: TriplestoreConfig) extends ListManager(config) with PlatformLoader
  with Store[Probe]{
  import QueryUtils._
  import Triplestore._

  val prefixes = s"$commonPrefixes PREFIX go:<http://www.geneontology.org/dtds/go.dtd#>"

  def defaultPrefix: String = ProbeStore.defaultPrefix
  def itemClass = ProbeStore.itemClass

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
    val platform = s"<${PlatformStore.defaultPrefix}/$platformName>"
    triplestore.simpleQuery(s"""$tPrefixes
                               |SELECT ?l WHERE {
                               |  GRAPH $platform {
                               |    ?p a ${ProbeStore.itemClass}; rdfs:label ?l .
                               |  }
                               |}""".stripMargin)
  }

  def allPlatforms = {
    val platformStore = new PlatformStore(config)
    ProbeStore.platformsAndProbes(platformStore, this)
  }

  /**
   * Obtain all probes in a given platform, with a few key annotations.
   * Probes will be potentially annotated with
   * entrez, refseq atranscript and symbol information only.
   */
  private def annotatedProbesForPlatform(platform: String): Iterable[Probe] = {
    val platformGraph = s"<${PlatformStore.defaultPrefix}/$platform>"
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
                   |SELECT ?pl
                   |  (GROUP_CONCAT(?ent; separator = ":") AS ?entCon)
                   |  (GROUP_CONCAT(?trn; separator = ":") AS ?trnCon)
                   |  (GROUP_CONCAT(?sym; separator = ":") AS ?symCon) WHERE {
                   |  GRAPH $platformGraph {
                   |    ?p a t:probe; rdfs:label ?pl.
                   |    OPTIONAL {
                   |      ?p t:entrez ?ent.
                   |      ?p t:refseqTrn ?trn.
                   |      ?p t:symbol ?sym.
                   |    }
                   |   } .
                   |}
                   |GROUP BY ?pl
                   |""".stripMargin

    val r = triplestore.mapQuery(query, 120000)

    (for {
      probe <- r;
      probeId = probe("pl");
      entrez = probe.get("entCon").toSeq.flatMap(_.split(":")).map(Gene(_));
      transcripts = probe.get("trnCon").toSeq.flatMap(_.split(":")).map(RefSeq(_));
      symbols = probe.get("symCon").toSeq.flatMap(_.split(":"));
      pr = Probe(probeId, genes = entrez.distinct,
        platform = platform,
        transcripts = transcripts.distinct,
        symbols = symbols.distinct)
    } yield pr)
  }

  /**
   * Obtain a full platform/probe lookup map.
   * It is preferred to query for a single platform only using probesForPlatform below
   * when possible.
   * @return
   */
  def platformsAndProbes: Map[String, Iterable[Probe]] = {
    val pfs = new PlatformStore(config)
    ProbeStore.platformsAndProbes(pfs, this)
  }

  def probesForPlatform(platform: String): Iterable[Probe] = {
    ProbeStore.loadOrFetchFromCache(this, platform)
  }

  def numProbes(): Map[String, Int] = {
    val r = triplestore.mapQuery(s"""$tPrefixes
                                    |SELECT (count(distinct ?p) as ?n) ?l WHERE {
                                    |  ?pl a ${PlatformStore.itemClass} ; rdfs:label ?l .
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
    if (rs.unresolved.size > 0) {
      val geneSyms = forGeneSyms(rs.unresolved, precise).allValues
      rs.resolve(geneSyms.map(_.identifier))
    }
  }

  protected def slowProbeResolution(rs: GradualProbeResolver,
                                    precise: Boolean): Unit = {
    val ups = forUniprots(rs.unresolved.map(p => Protein(p.toUpperCase))).map(_.identifier)
    rs.resolve(ups)
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
  def allGeneIds(): MMap[Probe, Gene] = {
    val query = s"""$prefixes
                   |SELECT DISTINCT ?p ?x WHERE {
                   |  GRAPH ?g {
                   |    ?p a t:probe ; t:entrez ?x .
                   |  }
                   |}""".stripMargin
    makeMultiMap(triplestore.mapQuery(query).map(x => (Probe.unpack(x("p")), Gene(x("x")))))
  }


  def forTitlePatterns(patterns: Iterable[String]): Iterable[Probe] = {
    val query = s"""$tPrefixes
                   |SELECT DISTINCT ?p WHERE {
                   |  GRAPH ?g {
                   |    ?p a t:probe ; rdfs:label ?l . """.stripMargin +
      caseInsensitiveMultiFilter("?l", patterns.map("\"" + _ + "\"")) +
      " } } "
    triplestore.simpleQuery(query).map(Probe.unpack)
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
  def probesForPartialSymbol(platform: Option[String], title: String): Vector[(String, String)] = {
    val query = s"""$prefixes
                   |SELECT DISTINCT ?s ?l WHERE {
                   |  ${platform.map(x => "?g rdfs:label \"" + x + "\".").getOrElse("")}
                   |  ?g a t:platform .
                   |  GRAPH ?g {
                   |    ?p a $itemClass; rdfs:label ?l.
                   |    OPTIONAL { ?p t:symbol ?s. }
                   |  }
                   |  FILTER (
                   |   REGEX(STR(?s), "^$title.*", "i") || REGEX(STR(?l), "^$title.*", "i")
                   |  )
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

  /**
   * Find GO terms matching the given string pattern.
   */
  def goTerms(pattern: String): Iterable[GOTerm] = {
    goTerms(pattern, 1000)
  }

  def goTerms(pattern: String, maxSize: Int): Iterable[GOTerm] = {

    val query = s"""$tPrefixes
                   |PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
                   |SELECT DISTINCT ?got ?gotn WHERE {
                   |  GRAPH <http://level-five.jp/t/annotation/go> {
                   |    ?got rdfs:label ?gotn; oboInOwl:id ?id.
                   |  } """.stripMargin +
      "FILTER regex(STR(?gotn), \".*" + pattern + ".*\", \"i\")" +
      s"} LIMIT ${maxSize}"
    triplestore.mapQuery(query).map(x => GOTerm(unpackGoterm(x("got")), x("gotn")))
  }

  //Task: A better solution is to have the URI of the GOTerm as a starting point to find the
  //probes, instead of looking for a matching name.
  def forGoTerm(term: GOTerm, platforms: List[String]): Iterable[Probe] = {
    val platformURIs = platforms.map(p =>  s"<${PlatformStore.defaultPrefix}/$p>")

    val query = s"""$tPrefixes
                   |PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
                   |SELECT DISTINCT ?probe WHERE {
                   |  GRAPH <http://level-five.jp/t/annotation/go> {
                   |    ?got rdfs:label "${term.name}"; oboInOwl:id ?id.
                   |  }
                   |  ?g2 a t:platform.
                   |  FILTER(?g2 IN (${platformURIs.mkString(", ")}))
                   |  GRAPH ?g2 {
                   |    ?probe a t:probe .
                   |    { ?probe t:gomf ?got . }
                   |      UNION { ?probe t:gocc ?got . }
                   |      UNION { ?probe t:gobp ?got . }
                   |      UNION { ?probe t:go ?got . }
                   |  }
                   |}""".stripMargin
    triplestore.simpleQuery(query, false, 30000).map(Probe.unpack)
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
                   |  GRAPH <http://level-five.jp/t/annotation/go> {
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
        s"?g a ${ProbeLists.itemClass}; ${InstanceStore.memberRelation} <$u>. ").getOrElse("") +
      s"?g ${ProbeLists.memberRelation} ?probeLabel; rdfs:label ?list. } " +
      //Note: string matching, such as in the following fragment, is impacted by
      //the handling of RDF1.0/1.1 strings (untyped/typed)
      // "?probe a t:probe; rdfs:label ?probeLabel. " + //filter out invalid probeLabels
      "}"

    //May be slow
    val mq = triplestore.mapQuery(q, 20000)
    makeMultiMap(mq.map(x => x("list") -> Probe(x("probeLabel"))))
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
}
