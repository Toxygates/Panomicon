package t.sparql

import t.TriplestoreConfig

object Datasets extends RDFClass {
   val defaultPrefix: String = s"$tRoot/dataset"
   val memberRelation = "t:visibleIn"
   val itemClass = "t:dataset"
}

class Datasets(config: TriplestoreConfig) extends BatchGroups(config) {
  import Triplestore._
  
  def memberRelation = Datasets.memberRelation
  def itemClass: String = Datasets.itemClass
  def groupClass = Datasets.itemClass
  def groupPrefix = Datasets.defaultPrefix
  def defaultPrefix = Datasets.defaultPrefix
    
  def descriptions:Map[String, String] = {
    Map() ++ ts.mapQuery(s"$tPrefixes select ?l ?desc where { ?item a $itemClass; rdfs:label ?l ; " +
        "t:description ?desc } ").map(x => {
          x("l") -> x("desc")
        })
  }
  
  def setDescription(name: String, desc: String) = {
    ts.update(s"$tPrefixes delete { <$defaultPrefix/$name> t:description ?desc } " +
        s"where { <$defaultPrefix/$name> t:description ?desc } ")
    ts.update(s"$tPrefixes insert data { <$defaultPrefix/$name> t:description " +
        "\"" + desc + "\" } ")
  }
}