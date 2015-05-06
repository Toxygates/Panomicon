package t.sparql

import t.TriplestoreConfig

object Instances extends RDFClass {
  val defaultPrefix = s"$tRoot/instance"
  val itemClass = "t:instance"
  val memberRelation = Batches.memberRelation
}

class Instances(config: TriplestoreConfig) extends ListManager(config) {  
  import Triplestore._
  def memberRelation = Instances.memberRelation 
  def defaultPrefix = Instances.defaultPrefix
  def itemClass: String = Instances.itemClass
    
  def listAccess(name: String): Seq[String] = {     
    ts.simpleQuery(s"$tPrefixes\n select ?bn where " +
        s"{ ?batch $memberRelation <$defaultPrefix/$name> . " +
        "?batch rdfs:label ?bn .}")
  }
  
  def enableAccess(name: String, batch: String): Unit = 
    new Batches(config).enableAccess(batch, name)
  
  def disableAccess(name: String, batch: String): Unit = 
    new Batches(config).disableAccess(batch, name)
    
  override def delete(name: String): Unit = {
    val upd = s"$tPrefixes delete { ?batch $memberRelation <$defaultPrefix/$name> } " +
    		s" where { ?batch $memberRelation <$defaultPrefix/$name> }"
    super.delete(name)
  }
}