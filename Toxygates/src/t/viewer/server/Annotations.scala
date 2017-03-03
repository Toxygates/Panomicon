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
import t.platform.ControlGroup
import t.common.shared.DataSchema
import t.sparql.TriplestoreMetadata
import t.sparql.SampleFilter
import t.db.Metadata
import t.viewer.server.Conversions._

class Annotations(sampleStore: Samples, schema: DataSchema, baseConfig: BaseConfig) {

//  private def bioParamsForSample(s: Sample): BioParameters =
//    Option(s.get(schema.timeParameter())) match {
//      case Some(t) => bioParameters.forTimePoint(t)
//      case _       => bioParameters
//    }

  //TODO this needs to update sometimes, ideally without restart
  lazy val bioParameters = {
    val pfs = new t.sparql.Platforms(baseConfig.triplestore)
    pfs.bioParameters
  }

  lazy val tsMeta = new TriplestoreMetadata(sampleStore, baseConfig.sampleParameters)(SampleFilter())

  def controlGroups(samples: Samples,
      ss: Iterable[Sample]): Map[Sample, ControlGroup] = {
     val cgs = ss.groupBy(_.get("control_group"))
    val mp = schema.mediumParameter()
    Map() ++ cgs.flatMap { case (cgroup, ss) =>
      val controls = ss.filter(s => schema.isControlValue(s.get(mp)))
      if (!controls.isEmpty) {
        val cg = new ControlGroup(bioParameters, tsMeta, controls.map(asScalaSample))
        ss.map(s => s -> cg)
      } else {
        Seq()
      }
    }
  }

   //TODO get these from schema, etc.
  def forSamples(samples: Samples, column: HasSamples[Sample],
      importantOnly: Boolean = false): Array[Annotation] = {

    val cgs = column.getSamples.groupBy(_.get("control_group"))

    val rs = for (
      (cgroup, ss) <- cgs;
      results = forSamples(samples, ss, importantOnly)
    ) yield results

    rs.flatten.toArray
  }

  /**
   * All samples part of the same control group
   */
  private def forSamples(sampleStore: Samples,
      samples: Iterable[Sample], importantOnly: Boolean) = {

    val (cg, keys) = if (importantOnly) {
      (None,
        baseConfig.sampleParameters.previewDisplay)
    } else {
      val mp = schema.mediumParameter()
      val controls = samples.filter(s => schema.isControlValue(s.get(mp)))
      if (controls.isEmpty) {
        (None,
          baseConfig.sampleParameters.previewDisplay)
      } else {
        (Some(new ControlGroup(bioParameters, tsMeta, controls.map(asScalaSample))),
          bioParameters.sampleParameters)
      }
    }
    samples.map(x => {
      val ps = sampleStore.parameterQuery(x.id, keys)
      fromParameters(cg, x, ps)
    })
  }

  def fromParameters(barcode: Sample,
    ps: Iterable[(t.db.SampleParameter, Option[String])]): Annotation = {

//    val controls = baseConfig.sampleParameters.controlSamples(tsMeta, asScalaSample(barcode))
//
//    val cg = if (controls.isEmpty) None else
//      Some(new ControlGroup(bioParameters, tsMeta, controls))
    fromParameters(None, barcode, ps)
  }

  private def fromParameters(cg: Option[ControlGroup], barcode: Sample,
    ps: Iterable[(t.db.SampleParameter, Option[String])]): Annotation = {

    def asJDouble(d: Option[Double]) =
      d.map(new java.lang.Double(_)).getOrElse(null)

    def bioParamValue(bp: BioParameter, dispVal: String) = {
      bp.kind match {
        case "numerical" =>
          val t = barcode.get(schema.timeParameter())
          val lb = cg.flatMap(_.lowerBound(bp.sampleParameter.identifier, t))
          val ub = cg.flatMap(_.upperBound(bp.sampleParameter.identifier, t))

          new NumericalBioParamValue(bp.key, bp.label,
          bp.section.getOrElse(null),
          asJDouble(lb), asJDouble(ub),
          dispVal)
        case _ => new StringBioParamValue(bp.key, bp.label,
          bp.section.getOrElse(null), dispVal)
      }
    }

//    val useBps = bioParamsForSample(barcode)

    val params = for (
      x <- ps.toSeq;
      bp <- bioParameters.get(x._1.identifier);
      p = (bp.label, x._2.getOrElse("N/A"));
      dispVal = OTGParameterSet.postReadAdjustment(p);
      bpv = bioParamValue(bp, dispVal)
    ) yield bpv

    new Annotation(barcode.id, new java.util.ArrayList(params))
  }

  //TODO use ControlGroup to calculate bounds here too
  def prepareCSVDownload(sampleStore: Samples, column: HasSamples[Sample],
      csvDir: String, csvUrlBase: String): String = {
    val ss = column.getSamples
    val timepoints = ss.toSeq.flatMap(s =>
      Option(s.get(schema.timeParameter()))).distinct

    val params = bioParameters.sampleParameters
    val raw = ss.map(x => {
      sampleStore.parameterQuery(x.id, params).toSeq
    })

    val colNames = params.map(_.humanReadable)

    val data = Vector.tabulate(raw.size, colNames.size)((s, a) =>
      raw(s)(a)._2.getOrElse(""))

    val bps = params.map(p => bioParameters.get(p.identifier))
    def extracts(b: Option[BioParameter], f: BioParameter => Option[String]): String =
      b.map(f).flatten.getOrElse("")

    def extractd(b: Option[BioParameter], f: BioParameter => Option[Double]): String =
      b.map(f).flatten.map(_.toString).getOrElse("")

    val rr = timepoints.map(t => {
      val bpt = bioParameters.forTimePoint(t)
      val bps = params.map(p => bpt.get(p.identifier))
      (Seq(s"Healthy min. $t", s"Healthy max. $t"),
          Seq(bps.map(extractd(_, _.lowerBound)), bps.map(extractd(_, _.upperBound))))
    })
    val helpRowTitles = Seq("Section") ++ rr.flatMap(_._1)
    val helpRowData = Seq(bps.map(extracts(_, _.section))) ++ rr.flatMap(_._2)

    CSVHelper.writeCSV("toxygates", csvDir: String, csvUrlBase: String,
      helpRowTitles ++ ss.map(_.id), colNames, helpRowData ++ data)
  }
}
