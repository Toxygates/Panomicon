package t.viewer.server

import java.lang.{ Double => JDouble }

import scala.collection.JavaConversions._
import scala.language.implicitConversions

import otg.db.OTGParameterSet
import t.BaseConfig
import t.common.shared.DataSchema
import t.common.shared.sample.Annotation
import t.common.shared.sample.HasSamples
import t.common.shared.sample.NumericalBioParamValue
import t.common.shared.sample.Sample
import t.common.shared.sample.StringBioParamValue
import t.model.sample.Attribute
import t.platform.BioParameter
import t.platform.ControlGroup
import t.sparql.Samples
import t.viewer.server.Conversions.asScalaSample
import t.sparql.TriplestoreMetadata
import t.sparql.SampleFilter
import t.db.Metadata
import t.viewer.server.Conversions._
import java.lang.{Double => JDouble}
import scala.language.implicitConversions
import t.sample.SampleSet
import t.model.sample.CoreParameter

class Annotations(val schema: DataSchema, val baseConfig: BaseConfig,
    unitHelper: Units) {

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

  import t.model.sample.CoreParameter.{ControlGroup => ControlGroupParam}

  implicit def asScala(x: Sample) = asScalaSample(x)

  /**
   * Find the control groups in a set of samples and create a map associating
   * samples with their control group
   * @param samples samples to partition
   * @param sampleSet a set that the discovered control samples have to be contained in
   */
  def controlGroups(samples: Iterable[Sample], sampleSet: SampleSet): Map[Sample, ControlGroup] = {
    val controlGroups = unitHelper.samplesByControlGroup(samples)
    val knownSamples = sampleSet.samples.map(_.sampleId).toSet
    Map() ++ controlGroups.flatMap { case (cgroup, ss) =>
      var (cs, ts) = ss.partition(s => schema.isControl(s))
      val controls = cs.map(asScalaSample).filter(s => knownSamples.contains(s.sampleId))
      if (!controls.isEmpty) {
        val cg = new ControlGroup(bioParameters, sampleSet, controls)
        ss.map(s => s -> cg)
      } else {
        Seq()
      }
    }
  }

  /**
   * Fetch annotations for given samples. Does not compute any bounds for
   * any parameters. Annotations will only be returned for samples that
   * have values for *all* of the attributes selected.
   * @param querySamples the samples for which to fetch annotations
   * @param queryAttributes the attributes to fetch
   */
  def forSamples(samples: Samples, querySamples: Iterable[Sample],
      queryAttribs: Iterable[Attribute]): Array[Annotation] = {
    val queryResult = samples.sampleAttributeValues(querySamples.map(_.id),
        queryAttribs)
    querySamples.map(s => fromAttributes(None, s, queryResult(s.id))).toArray
  }

  /**
   * For given samples, which may be in different control groups, fetch
   * annotations, using the control samples in the provided samples to
   * compute bounds for values if appropriate.
   * @param samples data source
   * @param column the samples for which we fetch annotations
   */
   //TODO get these from schema, etc.
  def forSamples(samples: Samples, querySet: Iterable[Sample],
      importantOnly: Boolean = false): Array[Annotation] = {

    samples.sampleAttributeValues(querySet.map(x => x.get(CoreParameter.SampleId)),
        List(otg.model.sample.Attribute.KidneyWeight, otg.model.sample.Attribute.LiverWeight))

    val cgs = querySet.groupBy(_(ControlGroupParam))

    val rs = for (
      (cgroup, ss) <- cgs;
      results = forSamplesSingleGroup(samples, ss, importantOnly)
    ) yield results

    rs.flatten.toArray
  }

  /**
   * Fetch annotations for samples from the same control group, using
   * provided control samples to generate bounds for variable, if applicable
   */
  private def forSamplesSingleGroup(sampleStore: Samples,
      samples: Iterable[Sample], importantOnly: Boolean) = {

    val (cg, keys) = if (importantOnly) {
      (None,
        baseConfig.attributes.getPreviewDisplay.toSeq)
    } else {
      val mp = schema.mediumParameter()
      val controls = samples.filter(s => schema.isControlValue(s.get(mp)))
      if (controls.isEmpty) {
        (None,
          bioParameters.sampleParameters)
      } else {
        (Some(new ControlGroup(bioParameters, sampleStore, controls.map(asScalaSample))),
          bioParameters.sampleParameters)
      }
    }
    samples.map(x => {
      val ps = sampleStore.parameterQuery(x.id, keys)
      fromAttributes(cg, x, ps)
    })
  }

  /**
   * Construct a shared API bio parameter object (numerical)
   */
  private def numericalAsShared(bp: BioParameter, lower: JDouble,
      upper: JDouble, value: String) =
    new NumericalBioParamValue(bp.key, bp.label, bp.section.getOrElse(null),
        lower, upper, value)

  /**
   * Construct a shared API bio parameter object (string)
   */
  private def stringAsShared(bp: BioParameter, value: String) =
    new StringBioParamValue(bp.key, bp.label, bp.section.getOrElse(null),
        value)

  /**
   * Construct an Annotation from sample attributes
   */
  def fromAttributes(sample: Sample,
    ps: Iterable[(Attribute, Option[String])]): Annotation = {
    fromAttributes(None, sample, ps)
  }

  /**
   * Construct an Annotation from sample attributes
   * @param cg control group; used to compute upper/lower bounds for parameters
   */
  private def fromAttributes(cg: Option[ControlGroup], sample: Sample,
    attribs: Iterable[(Attribute, Option[String])]): Annotation = {

    def asJDouble(d: Option[Double]) =
      d.map(new java.lang.Double(_)).getOrElse(null)

    def bioParamValue(bp: BioParameter, dispVal: String) = {
      bp.kind match {
        case "numerical" =>
          val t = sample.get(schema.timeParameter())
          val lb = cg.flatMap(_.lowerBound(bp.attribute, t, 1))
          val ub = cg.flatMap(_.upperBound(bp.attribute, t, 1))

          numericalAsShared(bp, asJDouble(lb), asJDouble(ub), dispVal)
        case _ => stringAsShared(bp, dispVal)
      }
    }

    val params = for (
      x <- attribs.toSeq;
      bp <- bioParameters.get(x._1);
      p = (bp.label, x._2.getOrElse("N/A"));
      bpv = bioParamValue(bp, p._2)
    ) yield bpv

    new Annotation(sample.id, new java.util.ArrayList(params))
  }

  //TODO use ControlGroup to calculate bounds here too
  def prepareCSVDownload(sampleStore: Samples, samples: Seq[Sample],
      csvDir: String, csvUrlBase: String): String = {
    val timepoints = samples.toSeq.flatMap(s =>
      Option(s.get(schema.timeParameter()))).distinct

    val params = bioParameters.sampleParameters
    val raw = samples.map(x => {
      sampleStore.parameterQuery(x.id, params).toSeq
    })

    val colNames = params.map(_.title)

    val data = Vector.tabulate(raw.size, colNames.size)((s, a) =>
      raw(s)(a)._2.getOrElse(""))

    val bps = params.map(p => bioParameters.get(p))
    def extracts(b: Option[BioParameter], f: BioParameter => Option[String]): String =
      b.map(f).flatten.getOrElse("")

    def extractd(b: Option[BioParameter], f: BioParameter => Option[Double]): String =
      b.map(f).flatten.map(_.toString).getOrElse("")

    val rr = timepoints.map(t => {
      val bpt = bioParameters.forTimePoint(t)
      val bps = params.map(p => bpt.get(p))
      (Seq(s"Healthy min. $t", s"Healthy max. $t"),
          Seq(bps.map(extractd(_, _.lowerBound)), bps.map(extractd(_, _.upperBound))))
    })
    val helpRowTitles = Seq("Section") ++ rr.flatMap(_._1)
    val helpRowData = Seq(bps.map(extracts(_, _.section))) ++ rr.flatMap(_._2)

    CSVHelper.writeCSV("toxygates", csvDir: String, csvUrlBase: String,
      helpRowTitles ++ samples.map(_.id), colNames, helpRowData ++ data)
  }
}
