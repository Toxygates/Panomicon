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
    for ((title, entries) <- byTitle; 
    		probes = entries.map(x => SimpleProbe(x("probe"), x("platform"))))
      yield OrthologGroup(title, probes)
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
  
  protected def simpleMapQuery(probes: Iterable[Probe], 
      query: String): MMap[Probe, DefaultBio] = {
    if (probes.isEmpty) {
      return emptyMMap()
    }
    
    val r = ts.mapQuery(query).map(x => Probe.unpack(x("probe")) -> 
      DefaultBio(x("result"), x("result")))
    makeMultiMap(r)
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
        .map(Probe.unpack).toSeq
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
  
  def forGoTerm(term: GOTerm): Iterable[Probe] = {
    val query = tPrefixes + 
    "SELECT DISTINCT ?probe WHERE { GRAPH ?g { " +
    "?probe a t:probe " + 
    "{" +
    "?got go:synonym ?gosyn. FILTER(?gosyn = \"" + term.name + "\") ." +
    "} UNION {" +
    "?got go:name ?gonam. FILTER(?gonam = \"" + term.name + "\"). " +
    """} { 
    ?probe t:gomf ?got .
    } UNION {     
    ?probe t:gocc ?got .
    } UNION {     
    ?probe t:gobp ?got .
    } } } """
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
    "SELECT DISTINCT ?got ?gotname ?probe WHERE { GRAPH ?g { " +
    "?probe a t:probe . " + 
    constraint + 
    multiFilter("?probe", probes.map(p => bracket(p.pack))) +
    """ FILTER STRSTARTS(STR(?got), "http://bio2rdf.org") 
    } GRAPH ?g2 {
      ?got2 owl:sameAs ?got. 
    } GRAPH ?g3 {
      ?got2 rdfs:label ?gotname. 
    } } """ 
    
    val r = ts.mapQuery(query).map(x => Probe.unpack(x("probe")) -> GOTerm(unpackGoterm(x("got")), x("gotname")))
    makeMultiMap(r)
  } 
  
  def probeLists(instanceURI: Option[String]): MMap[String, Probe] = {
    val q = tPrefixes + 
    "SELECT DISTINCT ?list ?probeLabel WHERE { " +
    instanceURI.map(u => 
      s"?g a ${ProbeLists.itemClass}; ${Instances.memberRelation} <$u>. "
      ).getOrElse("") +
    s"?g ${ProbeLists.memberRelation} ?probeLabel; rdfs:label ?list. " +
    "?probe a t:probe; rdfs:label ?probeLabel. " + //filter out invalid probeLabels
    "}"
    
    val mq = ts.mapQuery(q)
//    val byGroup = makeMultiMap(mq.map(x => x("l") -> Gene(x("entrez"))))    
//    val probes = forGenes(byGroup.values.flatten.toList.distinct)
    makeMultiMap(mq.map(x => x("list") -> Probe(x("probeLabel"))))        
  }
}