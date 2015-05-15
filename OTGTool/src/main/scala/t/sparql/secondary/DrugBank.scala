package t.sparql.secondary
import otg.Species._
import t.sparql._

class DrugBank extends Triplestore with CompoundTargets {
  
  val con = Triplestore.connectSPARQLRepository("http://drugbank.bio2rdf.org/sparql")

  val prefixes = """
    PREFIX drugbank:<http://bio2rdf.org/drugbank_vocabulary:>
"""

  def targetsFor(c: Compound, species: Option[Species] = None): Vector[Protein] = {
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
  
  def targetingFor(ps: Iterable[Protein], expected: Iterable[Compound], 
      species: Option[Species] = None):
    MMap[Protein, Compound] = {
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