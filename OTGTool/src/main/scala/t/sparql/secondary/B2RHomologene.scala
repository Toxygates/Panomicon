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
