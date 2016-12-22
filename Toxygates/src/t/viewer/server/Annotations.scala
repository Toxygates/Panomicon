package t.viewer.server

import t.common.shared.sample.NumericalBioParamValue
import t.platform.BioParameters
import t.platform.BioParameter
import t.common.shared.sample.StringBioParamValue
import t.common.shared.sample.Annotation
import t.common.shared.sample.Sample
import otg.db.OTGParameterSet
import t.BaseConfig
import scala.collection.JavaConversions._
import t.common.shared.sample.HasSamples
import t.sparql.Samples

class Annotations(baseConfig: BaseConfig) {
  def bioParamsForSample(s: Sample): BioParameters =
    Option(s.get("exposure_time")) match {
      case Some(t) => bioParameters.forTimePoint(t)
      case _       => bioParameters
    }

  //TODO this needs to update sometimes, ideally without restart
  lazy val bioParameters = {
    val pfs = new t.sparql.Platforms(baseConfig.triplestore)
    pfs.bioParameters
  }

   //TODO get these from schema, etc.
  def forSamples(samples: Samples, column: HasSamples[Sample],
      importantOnly: Boolean = false): Array[Annotation] = {
    val keys = if (importantOnly) {
      baseConfig.sampleParameters.previewDisplay
    } else {
      bioParameters.sampleParameters
    }

    column.getSamples.map(x => {
      val ps = samples.parameterQuery(x.id, keys)
      fromParameters(x, ps)
    })
  }

  def fromParameters(barcode: Sample,
    ps: Iterable[(t.db.SampleParameter, Option[String])]): Annotation = {

    def asJDouble(d: Option[Double]) =
      d.map(new java.lang.Double(_)).getOrElse(null)

    def bioParamValue(bp: BioParameter, dispVal: String) = {
      bp.kind match {
        case "numerical" => new NumericalBioParamValue(bp.key, bp.label,
          bp.section.getOrElse(null),
          asJDouble(bp.lowerBound), asJDouble(bp.upperBound),
          dispVal)
        case _ => new StringBioParamValue(bp.key, bp.label,
          bp.section.getOrElse(null), dispVal)
      }
    }

    val useBps = bioParamsForSample(barcode)

    val params = for (
      x <- ps.toSeq;
      bp <- useBps.get(x._1.identifier);
      p = (bp.label, x._2.getOrElse("N/A"));
      dispVal = OTGParameterSet.postReadAdjustment(p);
      bpv = bioParamValue(bp, dispVal)
    ) yield bpv

    new Annotation(barcode.id, new java.util.ArrayList(params))
  }

  def prepareCSVDownload(samples: Samples, column: HasSamples[Sample],
      csvDir: String, csvUrlBase: String): String = {
    val ss = column.getSamples
    val raw = ss.map(x => {
      samples.parameterQuery(x.id, bioParameters.sampleParameters).toSeq
    })

    val params = raw.head.map(_._1).toSeq

    //TODO sort columns by section and name
    val colNames = params.map(_.humanReadable)
    val data = Vector.tabulate(raw.size, colNames.size)((s, a) =>
      raw(s)(a)._2.getOrElse(""))

    val bps = params.map(p => bioParameters.get(p.identifier))
    def extracts(b: Option[BioParameter], f: BioParameter => Option[String]): String =
      b.map(f).flatten.getOrElse("")

    def extractd(b: Option[BioParameter], f: BioParameter => Option[Double]): String =
      b.map(f).flatten.map(_.toString).getOrElse("")

    val preRows = Seq(
      bps.map(extracts(_, _.section)),
      bps.map(extractd(_, _.lowerBound)),
      bps.map(extractd(_, _.upperBound)))

    CSVHelper.writeCSV("toxygates", csvDir: String, csvUrlBase: String,
      Seq("Section", "Healthy min.", "Healthy max.") ++ ss.map(_.id), colNames, preRows ++ data)
  }
}
