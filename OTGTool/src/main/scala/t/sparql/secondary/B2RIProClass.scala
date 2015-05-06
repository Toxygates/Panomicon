package t.sparql.secondary
import t.sparql._

class B2RIProClass extends Triplestore {  
  
  val con = Triplestore.connectSPARQLRepository("http://iproclass.bio2rdf.org/sparql")
  
  val prefixes = """
    PREFIX ipc:<http://bio2rdf.org/iproclass_vocabulary:>
"""
    
//    import scala.collection.{ Map => CMap, Set => CSet }
  def geneIdsFor(uniprots: Iterable[Protein]): MMap[Protein, Gene] = {
    val r = multiQuery(prefixes + "SELECT DISTINCT ?up ?gi WHERE { " +
        "?up ipc:x-geneid ?gi. " +
        multiFilter("?up", uniprots.map(p => bracket(p.packB2R))) +
            " } ").map(x => (Protein.unpackB2R(x(0)) -> Gene(unpackGeneid(x(1)))))
    makeMultiMap(r)            
  }  
  
  
  def main(args: Array[String]) {
    try {
	  val r = geneIdsFor(List(Protein("Q197F8")))
	  println(r)
	  println(r.size)
    } finally {
	  close()
    }
  }
}