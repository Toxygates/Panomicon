package t.model.sample

import t.db.Sample
import t.db.Metadata
import scala.collection.JavaConversions._



object Helpers { 

  implicit class AttributeSetHelper(attr: AttributeSet) {
    
    lazy val byIdLowercase = Map() ++ attr.byId.map(a => a._1.toLowerCase() -> a._2)
    
    def getById(id: String): Option[Attribute] = 
      Option(attr.byId(id))
      
    def getByTitle(title: String): Option[Attribute] =
      Option(attr.byTitle(title))
    
    /**
     * Identify the control samples belonging to a treated sample.
     */
    def controlSamples(metadata: Metadata, s: Sample): Iterable[Sample] = Seq()

    /**
     * Construct groups of treated and control samples.
     * @param ss treated samples that the groups are to be based on.
     */
    def treatedControlGroups(metadata: Metadata, ss: Iterable[Sample]): 
      Iterable[(Iterable[Sample], Iterable[Sample])] = {
      ss.groupBy(controlSamples(metadata, _)).toSeq.map(sg => {
        sg._2.partition(!metadata.isControl(_))
      })
    }
  }
}