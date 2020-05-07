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

import java.io.File

import t.TriplestoreConfig
import t.platform.ProbeRecord
import t.util.TempFiles
import t.platform.BioParameter
import t.platform.BioParameters
import t.model.sample.AttributeSet
import t.BaseConfig

object Platforms extends RDFClass {
  def itemClass: String = "t:platform"
  def defaultPrefix: String = s"$tRoot/platform"

  val platformType = "t:platformType"
  val biologicalPlatform = "t:biological"

  def context(name: String) = defaultPrefix + "/" + name
}

class Platforms(baseConfig: BaseConfig) extends
  ListManager(baseConfig.triplestore) {
  import Triplestore._
  import Platforms._

  def config = baseConfig.triplestore
  def itemClass = Platforms.itemClass
  def defaultPrefix = Platforms.defaultPrefix

  def redefine(name: String, comment: String, biological: Boolean,
      definitions: Iterable[ProbeRecord]): Unit = {
    delete(name) //ensure probes are removed
    addWithTimestamp(name, comment)

    if (biological) {
      triplestore.update(s"$tPrefixes\n insert data { <$defaultPrefix/$name> $platformType $biologicalPlatform. }")
    }

    val probes = new ProbeStore(config)

    val tempFiles = new TempFiles()
    try {
      for (g <- definitions.par.toList.grouped(1000)) {
        val ttl = ProbeStore.recordsToTTL(tempFiles, name, g)
        triplestore.addTTL(ttl, Platforms.context(name))
      }
    } finally {
      tempFiles.dropAll
    }
  }

  def redefineFromEnsembl(name: String, comment: String, inputFile: String) = {
    //Note: we are not currently auto-deleting the platform here
    //as a manual insert step is expected prior to running this function.
//    delete(name)
    addWithTimestamp(name, comment)

    //Adding the file through the SPARQLConnection currently is very inefficient - better to add the file
    //with HTTP directly via e.g. curl (scripts/triplestore/replace.sh can be used) instead of the below
//    triplestore.addTTL(new File(inputFile), Platforms.context(name))

    EnsemblPlatform.constructPlatformFromEnsemblGraph(triplestore, Platforms.context(name))
  }

  /**
   * Note, the map may only be partially populated
   */
  def platformTypes: Map[String, String] = {
    Map() ++ triplestore.mapQuery(s"$tPrefixes\nSELECT ?l ?type WHERE { ?item a $itemClass; rdfs:label ?l ; " +
      s"$platformType ?type } ").map(x => {
      x("l") -> x("type")
    })
  }

  private def removeProbeAttribPrefix(x: String) =
    if (x.startsWith(ProbeStore.probeAttributePrefix + "/")) {
      x.drop(ProbeStore.probeAttributePrefix.size + 1)
    } else {
      x
    }

  /**
   * Create those attributes in AttributeSet that did not already exist there,
   * but that are referenced by the triplestore.
   */
  def populateAttributes(into: AttributeSet): Unit = {
    val timeout: Int = 60000
     val attribs = triplestore.mapQuery(s"""$tPrefixes
        |SELECT DISTINCT * WHERE {
        |  ?p $platformType $biologicalPlatform.
        |  GRAPH ?p {
        |    ?probe a t:probe; rdfs:label ?id; t:type ?type; t:label ?title.
        |    OPTIONAL { ?probe t:section ?section. }
        |  }
        |}""".stripMargin, timeout)

     for (a <- attribs) {
       val at = into.findOrCreate(a("id"), a("title"), a("type"),
           a.get("section").orNull)
       println(s"Create attribute $at")
     }
  }

  /**
   * Obtain the bio-parameters in all bio platforms
   */
  def bioParameters: BioParameters = {
    val timeout: Int = 60000

    val attribs = triplestore.mapQuery(s"""$tPrefixes
        |SELECT ?id ?key ?value WHERE {
        |  ?p a t:platform; t:platformType t:biological.
        |  GRAPH ?p {
        |    ?probe a t:probe; rdfs:label ?id; ?key ?value.
        |  }
        |}""".stripMargin, timeout).map(x => (x("id"),
            removeProbeAttribPrefix(x("key")) -> x("value"))).groupBy(_._1)

    val attribMaps = for (
      (id, values) <- attribs;
      kvs = Map() ++ values.map(_._2)
    ) yield (id -> kvs)

    val bps = triplestore.mapQuery(s"""$tPrefixes
      |SELECT ?id ?desc ?sec ?type ?lower ?upper WHERE {
      |  ?p a t:platform; t:platformType t:biological.
      |  GRAPH ?p {
      |    ?probe a t:probe; rdfs:label ?id; t:label ?desc; t:type ?type.
      |    OPTIONAL { ?probe t:lowerBound ?lower; t:upperBound ?upper. }
      |    OPTIONAL { ?probe t:section ?sec. }
      |   }
      |}""".stripMargin, timeout)

    val attribSet = otg.model.sample.AttributeSet.getDefault

    val bpcons = bps.map(x => BioParameter(
        attribSet.findOrCreate(x("id"), x("desc"), x("type")),
      x.get("sec"),
      x.get("lower").map(_.toDouble), x.get("upper").map(_.toDouble),
      attribMaps.getOrElse(x("id"), Map())))

    new BioParameters(Map() ++ bpcons.map(b => b.attribute -> b))
  }

  override def delete(name: String): Unit = {
    super.delete(name)
    triplestore.update(s"$tPrefixes\n " +
      s"drop graph <$defaultPrefix/$name>")
  }

}

object EnsemblPlatform {
  val prefixes =
    """
      |PREFIX obo: <http://purl.obolibrary.org/obo/>
      |PREFIX purl: <http://www.purl.org/>
      |PREFIX dc: <http://purl.org/dc/elements/1.1/>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |PREFIX t:<http://level-five.jp/t/>
      |PREFIX ident_type: <http://idtype.identifiers.org/>
      |PREFIX identifiers: <http://identifiers.org/>
      |PREFIX sio: <http://semanticscience.org/resource/>
      |PREFIX term: <http://rdf.ebi.ac.uk/terms/ensembl/>
      |""".stripMargin

  def constructPlatformFromEnsemblGraph(ts: Triplestore, graph: String) : Unit = {
    //Run a series of SPARQL updates to construct the platform.
    //We construct one or a few attributes at a time for the probes, as generating them all at once
    //would cause a size explosion in the number of triples generated by each query.

    //Construct probes

    ts.update(
      s"""$prefixes
        |WITH <$graph>
        |INSERT {
        |  ?probe a t:probe; rdfs:label ?rsident;
        |  t:ensemblTranscript ?transcript; t:refseqTrn ?rsident.
        |} WHERE {
        |  ?transcript rdfs:seeAlso ?refseq.
        |  ?refseq a identifiers:refseq; dc:identifier ?rsident.
        |  BIND(IRI(CONCAT("http://level-five.jp/t/probe/", ?rsident)) as ?probe)
        |}
        |""".stripMargin
    )

    //genes
    ts.update(
      s"""$prefixes
         |WITH <$graph>
         |INSERT {
         |  ?probe t:ensemblGene ?gene.
         |} WHERE {
         |  ?probe t:ensemblTranscript ?transcript.
         |  ?transcript obo:SO_transcribed_from ?gene.
         |}
        """.stripMargin
    )

    //proteins
    ts.update(
      s"""$prefixes
         |WITH <$graph>
         |INSERT {
         |  ?probe t:ensemblProtein ?protein.
         |} WHERE {
         |  ?probe t:ensemblTranscript ?transcript.
         |  ?transcript obo:SO_translates_to ?protein.
         |}
        """.stripMargin
    )


    //Uniprot
    //Note: filtering on purl.uniprot.org is a little ugly but there was no simpler way of achieving this result
    //where the query would complete in acceptable time.
    ts.update(
      s"""$prefixes
        |WITH <$graph>
        |INSERT {
        |  	?probe t:swissprot ?upident.
        |} WHERE {
        |   ?probe t:ensemblProtein ?protein.
        |   ?protein term:DIRECT ?uniprot.
        |   ?uniprot dc:identifier ?upident.
        |  FILTER(contains(str(?uniprot), "purl.uniprot.org"))
        |}
        """.stripMargin
    )

    //Titles
    ts.update(
      s"""$prefixes
        |WITH <$graph>
        |INSERT {
        |  ?probe t:title ?title.
        |} WHERE {
        |  ?probe t:ensemblProtein ?protein.
        |  ?protein term:DIRECT [ dc:description ?title ].
        |  FILTER(?title NOT IN("Generated via direct", "Generated via otherfeatures"))
        |}
        """.stripMargin
    )

    //Gene symbols
    ts.update(
      s"""$prefixes
         |WITH <$graph>
         |INSERT {
         | 	?probe t:symbol ?symbol.
         |} WHERE {
         |  ?probe t:ensemblGene ?gene.
         |  ?gene rdfs:seeAlso [ rdfs:label ?symbol ].
         |}
        """.stripMargin
    )

    //Entrez/NCBI genes
    ts.update(
      s"""$prefixes
         |WITH <$graph>
         |INSERT {
         |  ?probe t:entrez ?ncbiident.
         |} WHERE {
         |  ?probe t:ensemblGene ?gene.
         |  ?gene term:DEPENDENT ?ncbigene. ?ncbigene a identifiers:ncbigene; dc:identifier ?ncbiident.
         |}
         |""".stripMargin
    )
  }

}