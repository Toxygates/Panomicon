package otg.platform
import scala.io._
import java.io._
import t.sparql.secondary.Gene
import scala.collection.mutable.{HashMap => MHMap, Set => MSet}
import otg.sparql.Probes
import scala.collection.mutable.{HashMap => MHMap}
import scala.collection.mutable.{Set => MSet}
import t.platform.Probe

/**
 * Convert SSearch similarity files to TTL format, by using
 * already inserted platform information.
 */
class SSOrthTTL(probes: Probes, 
    inputs: Iterable[String], output: String) {

  val probeToGene = probes.allGeneIds()
  val geneToProbe = probeToGene.reverse

  def generate(): Unit = {
    val all = new MHMap[Probe, MSet[Probe]]
    var allList = List[MSet[Probe]]()
    
	for (i <- inputs; p <- readPairs(i)) {
	    val ps1 = geneToProbe.get(p._1)
	    val ps2 = geneToProbe.get(p._2)
	    
	    /**
	     * This builds the transitive closure of all the 
	     * ortholog relations
	     */
	    if (ps1 != None && ps2 != None) {
	      val nw = ps1.get ++ ps2.get
	      val existing = nw.find(all.contains(_))
	      existing match {
	        case Some(e) => all(e) ++= nw
	        case None => {
	          val nset = MSet() ++ nw
	          all ++= nset.toSeq.map(x => (x -> nset))
	          allList ::= nset
	        }
	      }	      
	  }	  
	}
    
    var fw: BufferedWriter = null
    try {
      fw = new BufferedWriter(new FileWriter(output))
      fw.write("@prefix t:<http://level-five.jp/t/>. ")
      fw.newLine()
      val pre = t.sparql.Probes.defaultPrefix
      val rel = "t:hasOrtholog"
      for (vs <- allList) {
        fw.write(s"[] $rel ")        
        fw.write(vs.toList.map(v => s"<$pre/${v.identifier}>").mkString(", "))
        fw.write(".")
        fw.newLine()        
      }
      
    } finally {
      if (fw != null) fw.close()      
    }
  }
  
  /**
   * Read pairs of ENTREZ ids
   */
  def readPairs(in: String): Iterable[(Gene, Gene)] = {
    Source.fromFile(in).getLines.toVector.flatMap(l => {
    	val gs = l.split("\t")
    	if (gs.length != 2) {
    	  None
    	} else if (gs(0) != "-" && gs(1) != "-") {
    	  val i1 = Integer.parseInt(gs(0))
    	  val i2 = Integer.parseInt(gs(1))
    	  //standard sort order
    	  if (i1 < i2) {
    	    Some((Gene(i1), Gene(i2)))
    	  } else {
    	    Some((Gene(i2), Gene(i1)))
    	  }
    	} else {
    	  None
    	}
    })
  }
}