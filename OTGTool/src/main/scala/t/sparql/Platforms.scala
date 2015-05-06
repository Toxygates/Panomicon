package t.sparql

import t.TriplestoreConfig
import t.platform.ProbeRecord
import t.util.TempFiles

object Platforms extends RDFClass {
  def itemClass: String = "t:platform"
  def defaultPrefix: String = s"$tRoot/platform"  
  
  def context(name: String) = defaultPrefix + "/" + name
}

class Platforms(config: TriplestoreConfig) extends ListManager(config) with TRDF {
  import Triplestore._
  import Platforms._
  
  def itemClass = Platforms.itemClass
  def defaultPrefix = Platforms.defaultPrefix
  
  def redefine(name: String, comment: String, definitions: Iterable[ProbeRecord]): Unit = {
    delete(name) //ensure probes are removed
    addWithTimestamp(name, comment)
    val probes = new Probes(config)
    
    val tempFiles = new TempFiles()
    try {
      for (g <- definitions.par.toList.grouped(1000)) {
        val ttl = Probes.recordsToTTL(tempFiles, name, g)
        ts.addTTL(ttl, Platforms.context(name))
      }
    } finally {
      tempFiles.dropAll
    }
  }

  override def delete(name: String): Unit = {
	super.delete(name)	
	ts.update(s"$tPrefixes\n " +
        s"drop graph <$defaultPrefix/$name>")
  }
  
}