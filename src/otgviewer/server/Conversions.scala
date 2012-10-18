package otgviewer.server

import otgviewer.shared.DataFilter
import otgviewer.shared.Pathology
import otg.BCode
import otgviewer.shared.Barcode
import otg.Species
import otgviewer.shared.Organism
import otgviewer.shared.Annotation
import scala.collection.JavaConversions._

object Conversions {

  implicit def asScala(filter: DataFilter): otg.Filter = new otg.Filter(filter.cellType.toString(), filter.organ.toString(),
      filter.repeatType.toString(), filter.organism.toString());
  
  implicit def asJava(path: otg.Pathology): Pathology = 
    new Pathology(path.barcode, path.topography, path.finding, path.spontaneous, path.grade);
  
  implicit def asJava(annot: otg.Annotation): Annotation = 
    new Annotation(annot.barcode, new java.util.ArrayList(annot.data.map(x => new Annotation.Entry(x._1, x._2))))
  
  def asJava(bcode: BCode, compound: String): Barcode = new Barcode(bcode.code, bcode.individual, bcode.dose,
					bcode.time, compound);
  
  implicit def speciesFromFilter(filter: DataFilter): Species = {
    filter.organism match {
      case Organism.Rat => otg.Rat()
      case Organism.Human => otg.Human()
    }
  }
  
}