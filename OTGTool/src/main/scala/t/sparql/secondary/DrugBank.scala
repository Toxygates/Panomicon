/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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
import otg.Species._
import t.sparql._

class DrugBank extends Triplestore with CompoundTargets {

  val con = Triplestore.connectSPARQLRepository("http://drugbank.bio2rdf.org/sparql")

  val prefixes = """
    PREFIX drugbank:<http://bio2rdf.org/drugbank_vocabulary:>
"""

  def targetsFor(c: Compound): Vector[Protein] = {
    simpleQuery(prefixes + " SELECT DISTINCT ?r WHERE {" +
      """?s rdfs:label ?l ;  
		  		a drugbank:Drug ;
		  		drugbank:target ?t .
		  		?t drugbank:xref ?r .
		  		filter regex(?r, "uniprot", "i")""" +
      "filter regex(?l, \"^" + c.name + "\", \"i\") " +
      //"filter (lcase(?l) = \"" + drug.toLowerCase() + "\" )" + 
      //filter regex(?l, """" + drug + "\", \"i\") 
      " }").map(Protein.unpackB2R(_))
  }

  def targetingFor(ps: Iterable[Protein], expected: Iterable[Compound]): MMap[Protein, Compound] = {
    val cmps = expected.toSet
    val r = multiQuery(prefixes + " SELECT DISTINCT ?r ?sa " +
      """ (lcase(replace(?l, " \\[drugbank:.*\\]", "")) as ?lc) """ +
      " WHERE { " +
      """?s rdfs:label ?l ; 
          rdfs:seeAlso ?sa ;
          a drugbank:Drug ;
          drugbank:target ?t .
          ?t drugbank:xref ?r . """ +
      " FILTER REGEX(?sa, \"drugbank\") \n" +
      multiFilter("?r", ps.map(p => bracket(p.packB2R)).toSet) +
      "}")(20000)
    val rr = r.map(x => (Protein.unpackB2R(x(1)) -> Compound.make(x(2)).copy(identifier = x(0))))
    makeMultiMap(rr).mapValues(_.intersect(cmps))
  }
}
