package t.platform

import t.db.file.TSVMetadata
import t.db.file.MapMetadata
import t.db.Metadata
import t.db.Sample
import t.BaseConfig
import org.apache.commons.math3.stat.StatUtils.variance
import org.apache.commons.math3.stat.StatUtils.mean
import t.sample.SampleSet
import t.model.sample.Attribute
import t.model.sample.BasicAttribute
import otg.model.sample.Attribute._
import scala.collection.JavaConversions._

/**
 * Construct a BioParameter object.
 * @param attributes attributes of the bio parameter (not sample attributes) -
 *  tentative functionality for describing bio parameters in platforms
 */
case class BioParameter(attribute: Attribute,
    section: Option[String],
    lowerBound: Option[Double], upperBound: Option[Double],    
    attributes: Map[String, String] = Map()) {

  def key: String = attribute.id

  def label: String = attribute.title

  def kind: String = {
    if (attribute.isNumerical()) "numerical" else "text"
  }
}

class BioParameters(lookup: Map[Attribute, BioParameter]) {
  def apply(key: Attribute) = lookup(key)
  def get(key: Attribute) = lookup.get(key)

  /**
   * Obtain the set as attributes, sorted by section and label.
   */
  def sampleParameters: Seq[Attribute] = lookup.values.toSeq.
    sortBy(p => (p.section, p.label)).map(_.attribute)

  /**
   * Extract bio parameters with accurate low and high threshold for a given
   * time point. The raw values are stored in the root parameter set's
   * attribute maps.
   * @param time The time point, e.g. "24 hr"
   */
  def forTimePoint(time: String): BioParameters = {
    val normalTime = time.replaceAll("\\s+", "")

    new BioParameters(Map() ++
      (for (
        (attr, param) <- lookup;
        lb = param.attributes.get(s"lowerBound_$normalTime").
          map(_.toDouble).orElse(param.lowerBound);
        ub = param.attributes.get(s"upperBound_$normalTime").
          map(_.toDouble).orElse(param.upperBound);
        edited = BioParameter(attr, param.section, lb, ub,
          param.attributes)
      ) yield (attr -> edited)))
  }

  def all = lookup.values
}

/**
 * Helper to compute statistical values for samples belonging to the same treated/control group.
 * Will eagerly retrieve and process all parameters for the samples upon construction.
 */
class ControlGroup(bps: BioParameters, samples: SampleSet,
    val controlSamples: Iterable[Sample]) {
  val byTime = controlSamples.groupBy(s => samples.parameter(s, ExposureTime))

  val allParamVals = byTime.filter(_._1 != None).
    map(ss => ss._1.get -> ss._2.map(Map() ++ samples.parameters(_)))

  private def varAndMean(param: Attribute, time: String): Option[(Double, Double)] = {
    allParamVals.get(time) match {
      case None => None
      case Some(pvs) =>
        val vs = pvs.flatMap(_.get(param))
        val nvs = vs.flatMap(BioParameter.convert)

        if (nvs.size < 2) {
          None
        } else {
          Some((variance(nvs.toArray), mean(nvs.toArray)))
        }
    }
  }

  def lowerBound(param: Attribute, time: String, testSampleSize: Int): Option[Double] =
    varAndMean(param, time).map {
      case (v, m) =>
        val sd = Math.sqrt(v)
        m - 2 / Math.sqrt(testSampleSize) * sd
    }

  def upperBound(param: Attribute, time: String, testSampleSize: Int): Option[Double] =
    varAndMean(param, time).map {
      case (v, m) =>
        val sd = Math.sqrt(v)
        m + 2 / Math.sqrt(testSampleSize) * sd
    }
}

object BioParameter {

  def convert(x: String) = x match {
      case "NA" => None
      case _    => Some(x.toDouble)
    }

  def main(args: Array[String]) {
     val f = new otg.Factory
     val attrs = otg.model.sample.AttributeSet.getDefault
     val data = TSVMetadata(f, args(0), attrs)
     var out = Map[Attribute, Seq[String]]()

     for (time <- data.parameterValues(ExposureTime.id)) {
       val ftime = time.replaceAll("\\s+", "")
       var samples = data.samples
       samples = samples.filter(s => {
         val m = data.parameterMap(s)
         m(ExposureTime.id) == time && m(DoseLevel.id) == "Control" && m("test_type") == "in vivo"
       })
       val rawValues = samples.map(s => data.parameters(s))
       for (attr <- attrs.getAll; if attr.isNumerical()) {
         if (!out.contains(attr)) {
           out += attr -> Seq()
         }

        val rawdata = rawValues.map(_.find( _._1 == attr).get).map(x => convert(x._2))
        if (!rawdata.isEmpty) {
          val v = variance(rawdata.flatten.toArray)
          val m = mean(rawdata.flatten.toArray)
          val sd = Math.sqrt(v)
          val upper = m + 2 * sd
          val lower = m - 2 * sd
          out += attr -> (out(attr) :+ s"lowerBound_$ftime=$lower")
          out += attr -> (out(attr) :+ s"upperBound_$ftime=$upper")
        }
       }
     }
     for ((k, vs) <- out) {
       println(k.id + "\t" + vs.mkString(","))
     }
  }
}
