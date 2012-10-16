package otgviewer.server

import otgviewer.shared.DataFilter
import otgviewer.shared.Pathology
import otg.BCode
import otgviewer.shared.Barcode
import otg.Species

object Conversions {

  implicit def asScala(filter: DataFilter): otg.Filter = new otg.Filter(filter.cellType.toString(), filter.organ.toString(),
      filter.repeatType.toString(), filter.organism.toString());
  
  implicit def asJava(path: otg.Pathology): Pathology = 
    new Pathology(path.barcode, path.topography, path.finding, path.spontaneous, path.grade);
  
  def asJava(bcode: BCode, compound: String): Barcode = new Barcode(bcode.code, bcode.individual, bcode.dose,
					bcode.time, compound);
  
  implicit def speciesFromFilter(filter: DataFilter): Species = Utils.speciesFromFilter(filter)
  
}