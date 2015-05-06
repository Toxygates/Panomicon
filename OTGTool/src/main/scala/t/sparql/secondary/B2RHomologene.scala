package t.sparql.secondary

import t.sparql._

class B2RHomologene extends Triplestore {    
  val con = Triplestore.connectSPARQLRepository("http://homologene.bio2rdf.org/sparql")

  val prefixes = """
    PREFIX hg:<http://bio2rdf.org/homologene_vocabulary:>
    PREFIX gi:<http://bio2rdf.org/geneid:> 
"""

  /**
   * For a given source gene (specified as Entrez number) obtain the
   * orthologous Entrez gene IDs in other species.
   */
  def homologousGenes(gene: Gene): Vector[Gene] = {
    simpleQuery(prefixes + " SELECT DISTINCT ?o WHERE { " +
      "?group hg:has_gene gi:" + gene.identifier + "; " +
      "  hg:has_gene ?o . } ").map(Gene.unpackId(_))
  }

  /**
   * For several different source genes (specified as Entrez number) obtain the
   * orthologous Entrez gene IDs in other species.
   * TODO consider using a UNION query instead
   */
  def homologousGenes(genes: Iterable[Gene]): MMap[Gene, Gene] = {
    val r = multiQuery(prefixes + " SELECT DISTINCT ?g ?o WHERE { " +
      "?group hg:has_gene ?g ; " +
      "  hg:has_gene ?o . " +
      multiFilter("?g", genes.map("gi:" + _.identifier)) + " . }").map(x => (Gene.unpackId(x(0)), Gene.unpackId(x(1))))
    makeMultiMap(r)
  }
 
  val iproClass = new B2RIProClass()

  def homologousGenesFor(uniprots: Iterable[Protein]): MMap[Protein, Gene] = {
    //first obtain the corresponding genes
    val geneIds = if (!uniprots.isEmpty) { iproClass.geneIdsFor(uniprots) } else { emptyMMap[Protein, Gene]() }
    val allGIs = geneIds.values.flatten
    val homologs = if (!allGIs.isEmpty) { homologousGenes(allGIs) } else { emptyMMap[Gene, Gene]() }
    geneIds.map(x => (x._1 -> x._2.flatMap(homologs.getOrElse(_, Set()))))
  }
  
  override def close {
    super.close
    iproClass.close
  }
}