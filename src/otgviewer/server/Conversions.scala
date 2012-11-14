package otgviewer.server

import scala.collection.JavaConversions._
import otg.BCode
import otg.SeriesMatching
import otg.Species
import otgviewer.shared.Annotation
import otgviewer.shared.Barcode
import otgviewer.shared.CellType
import otgviewer.shared.DataFilter
import otgviewer.shared.ExpressionValue
import otgviewer.shared.Organism
import otgviewer.shared.Pathology
import otgviewer.shared.RankRule
import otgviewer.shared.Series
import otgviewer.shared.RuleType

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
  
  def nullToOption[T](v: T): Option[T] = {
    if (v == null) {
      None
    } else {
      Some(v)
    }
  }

  implicit def asScala(rr: RankRule): SeriesMatching.MatchType = {
    
    rr.`type`() match {      
      case s: RuleType.Synthetic.type  => {
        println("Correlation curve: " + rr.data.toVector)
        SeriesMatching.MultiSynthetic(rr.data.toVector)
      }
      case r: RuleType.HighVariance.type => SeriesMatching.HighVariance()
      case r: RuleType.LowVariance.type => SeriesMatching.LowVariance()
      case r: RuleType.Sum.type => SeriesMatching.Sum()
      case r: RuleType.NegativeSum.type => SeriesMatching.NegativeSum()
      case r: RuleType.Unchanged.type => SeriesMatching.Unchanged()
      case r: RuleType.MonotonicUp.type => SeriesMatching.MonotonicIncreasing()
      case r: RuleType.MonotonicDown.type => SeriesMatching.MonotonicDecreasing()
    }
  }
  
  implicit def asJava[T,U](v: (T, U)) = new otgviewer.shared.Pair[T, U](v._1, v._2)
  
}