package t.sparql.secondary
import otg.Species._
import t.sparql.Triplestore
import t.sparql._
import otg.Species

trait CompoundTargets {
  def targetsFor(compound: Compound): Iterable[Protein]
  def targetingFor(ps: Iterable[Protein],  
      expected: Iterable[Compound]): MMap[Protein, Compound]
}

class ChEMBL extends Triplestore with CompoundTargets {
  
  val con = Triplestore.connectSPARQLRepository("https://www.ebi.ac.uk/rdf/services/chembl/sparql")

  val prefixes = commonPrefixes + """    
  	PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
  	PREFIX cco: <http://rdf.ebi.ac.uk/terms/chembl#>
  	PREFIX chembl_molecule: <http://rdf.ebi.ac.uk/resource/chembl/molecule/>
  	PREFIX up: <http://purl.uniprot.org/uniprot/>
"""

  def targetsFor(compound: Compound): Set[Protein] = {    
     val r = simpleQuery(prefixes +
      "SELECT ?uniprot WHERE { " +
      "?mol rdfs:label \"" + compound.name.toUpperCase() + "\". " + 
      """ ?target cco:organismName ?orgName. 
        ?activity a cco:Activity ;
      	cco:standardType ?t ;
      	cco:hasMolecule ?mol ;       
      	cco:hasAssay ?assay .
      	?assay cco:hasTarget ?target .
      	?target cco:hasTargetComponent ?targetcmpt .
      	?targetcmpt cco:targetCmptXref ?uniprot .
      	?uniprot a cco:UniprotRef .  
      	FILTER (?t IN("Inhibition", "Ki", "IC50")) """ +
        multiFilter("?orgName", Species.supportedSpecies.map(_.longName)) +
      	"}")(60000)           
      r.map(p => Protein.unpackUniprot(unbracket(p))).toSet        
  }
  
  private def capitalise(compound: String) = compound(0).toUpper + compound.drop(1).toLowerCase()

  def targetingFor(ps: Iterable[Protein], expected: Iterable[Compound]): MMap[Protein, Compound] = {
         val r = mapQuery(prefixes +
      """SELECT ?mol ?compound ?uniprot WHERE { 
      ?mol rdfs:label ?compound .
      ?target cco:organismName ?orgName .       
      ?activity a cco:Activity ;
      	cco:standardType ?t ;
      	cco:hasMolecule ?mol ;       
      	cco:hasAssay ?assay .
      	?assay cco:hasTarget ?target .
      	?target cco:hasTargetComponent ?targetcmpt .
      	?targetcmpt cco:targetCmptXref ?uniprot .
      	?uniprot a cco:UniprotRef .  
      	FILTER (?t IN("Inhibition", "Ki", "IC50"))))""" +
      	multiFilter("?uniprot", ps.map(p => "up:" + p.identifier).toSet)  +
      	multiFilter("?compound", expected.map(e => "\"" + e.name.toUpperCase + "\"")) +
        multiFilter("?orgName", Species.supportedSpecies.map(_.longName)) +
      	"}")(60000) 
      	
        makeMultiMap(r.map(x => (Protein.unpackUniprot(unbracket(x("uniprot"))) ->  
          Compound.unpackChEMBL(unbracket(x("mol"))).copy(name = capitalise(x("compound"))))))
  }
    
}