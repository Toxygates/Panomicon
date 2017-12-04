package t.model.sample

import scala.collection.JavaConversions._

object Helpers { 

  implicit class AttributeSetHelper(attr: AttributeSet) {
    
    lazy val byIdLowercase = Map() ++ attr.byId.map(a => a._1.toLowerCase() -> a._2)
    
    def getById(id: String): Option[Attribute] = 
      Option(attr.byId(id))
      
    def getByTitle(title: String): Option[Attribute] =
      Option(attr.byTitle(title))
  }
}