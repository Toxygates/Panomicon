package t.platform

import t.db.SampleParameter
import t.db.file.TSVMetadata
import t.db.file.MapMetadata

case class BioParameter(key: String, label: String, kind: String,
    section: Option[String],
    lowerBound: Option[Double], upperBound: Option[Double],
    attributes: Map[String, String] = Map()) {

  /**
   * Obtain an equivalent SampleParameter (for lookup purposes)
   */
  def sampleParameter = SampleParameter(key, label)
}

class BioParameters(lookup: Map[String, BioParameter]) {
  def apply(key: String) = lookup(key)
  def get(key: String) = lookup.get(key)

  def sampleParameters = lookup.values.map(_.sampleParameter)

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
        (id, param) <- lookup;
        lb = param.attributes.get(s"lowerBound_$normalTime").
          map(_.toDouble).orElse(param.lowerBound);
        ub = param.attributes.get(s"upperBound_$normalTime").
          map(_.toDouble).orElse(param.upperBound);
        edited = BioParameter(id, param.label, param.kind, param.section, lb, ub,
          param.attributes)
      ) yield (id -> edited)))
  }
}

object BioParameter {
  import org.apache.commons.math3.stat.StatUtils.variance
  import org.apache.commons.math3.stat.StatUtils.mean

  def convert(x: String) = x match {
      case "NA" => None
      case _    => Some(x.toDouble)
    }

  def main(args: Array[String]) {
     val f = new otg.Factory
     val params = otg.db.OTGParameterSet
     val data = TSVMetadata(f, args(0), params)
     var out = Map[SampleParameter, Seq[String]]()

     for (time <- data.parameterValues("exposure_time")) {
       val ftime = time.replaceAll("\\s+", "")
       var samples = data.samples
       samples = samples.filter(s => {
         val m = data.parameterMap(s)
         m("exposure_time") == time && m("dose_level") == "Control" && m("test_type") == "in vivo"
       })
       val rawValues = samples.map(s => data.parameters(s))
       for (param <- params.all; if params.isNumerical(param)) {
         if (!out.contains(param)) {
           out += param -> Seq()
         }

        val rawdata = rawValues.map(_.find( _._1 == param).get).map(x => convert(x._2))
        if (!rawdata.isEmpty) {
          val v = variance(rawdata.flatten.toArray)
          val m = mean(rawdata.flatten.toArray)
          val sd = Math.sqrt(v)
          val upper = m + 2 * sd
          val lower = m - 2 * sd
          out += param -> (out(param) :+ s"lowerBound_$ftime=$lower")
          out += param -> (out(param) :+ s"upperBound_$ftime=$upper")
        }
       }
     }
     for ((k, vs) <- out) {
       println(k.identifier + "\t" + vs.mkString(","))
     }
  }
}
