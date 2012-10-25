package otgviewer.server

import otgviewer.shared.CellType
import otgviewer.shared.DataFilter
import otgviewer.shared.Pathology
import otg.BCode
import otgviewer.shared.Barcode
import otg.Species
import otgviewer.shared.Organism
import otgviewer.shared.Annotation
import scala.collection.JavaConversions._
import otgviewer.shared.Series
import otgviewer.shared.ExpressionValue

object Conversions {
  import language.implicitConversions

  implicit def asScala(filter: DataFilter): otg.Filter = {
    val or = if (filter.cellType == CellType.Vitro) {
      otg.Vitro
    } else {
      otg.Organ(filter.organ.toString())
    }
    new otg.Filter(or, otg.RepeatType(filter.repeatType.toString()), otg.Species(filter.organism.toString()));
  }

  implicit def asJava(path: otg.Pathology): Pathology =
    new Pathology(path.barcode, path.topography, path.finding, path.spontaneous, path.grade);

  implicit def asJava(annot: otg.Annotation): Annotation =
    new Annotation(annot.barcode, new java.util.ArrayList(annot.data.map(x =>
      new Annotation.Entry(x._1, x._2, otg.Annotation.isNumerical(x._1)))))

  def asJava(bcode: BCode, compound: String): Barcode = new Barcode(bcode.code, bcode.individual, bcode.dose,
    bcode.time, compound);

  implicit def speciesFromFilter(filter: DataFilter): Species = {
    filter.organism match {
      case Organism.Rat   => otg.Rat
      case Organism.Human => otg.Human
    }
  }

  implicit def asScala(filter: DataFilter, series: Series): otg.Series = {
	val sf = asScala(filter)
	new otg.Series(sf.repeatType, sf.organ, sf.species, 
	    series.probe, series.compound, series.timeDose, Vector())
  }

  implicit def asJava(series: otg.Series): Series = {
	new Series(series.compound + " " + series.timeDose, series.probeStr, series.timeDose,
	    series.compound, series.data.map(asJava).toArray)
  }
  
  implicit def asJava(ev: otg.ExprValue): ExpressionValue = new ExpressionValue(ev.value, ev.call)
    
}