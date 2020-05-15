package t.db

import friedrich.util.formats.TSVFile
import t.Context
import t.platform.{EnsemblPlatformHelper, Species}

import scala.collection.{Map => CMap}

/**
 * ColumnExpressionData that converts probe IDs on the fly.
 * The supplied conversion map should map from the foreign ID space into the Toxygates space.
 *
 * In the case of a many-to-one mapping, one of the results will be selected in an undefined way.
 *
 * The "speculative" conversion map is a backup mapping with lower confidence that will be used
 * only if no primary mapping is available.
 */
class IDConverter(raw: ColumnExpressionData, conversion: Map[ProbeId, Iterable[ProbeId]],
                  speculativeConversion: Map[ProbeId, Iterable[ProbeId]] = Map()) extends
  ColumnExpressionData {

  IDConverter.checkDuplicates(conversion)

  lazy val probes = raw.probes.flatMap(p =>
    conversion.getOrElse(p,
      speculativeConversion.getOrElse(p, Seq())
    )).distinct

  def convert(p: ProbeId) = {
    conversion.getOrElse(p, {
      val speculative = speculativeConversion.getOrElse(p, Seq())
      if (speculative.isEmpty) {
        Console.err.println(s"Warning: could not convert the following probe ID: $p")
        Seq()
      } else {
        Console.err.println(s"Warning: speculative conversion of $p into ${speculative.mkString(" ")}")
        speculative
      }
    })
  }

  def data(s: Sample): CMap[ProbeId, FoldPExpr] = {
    val r = raw.data(s)
    r.flatMap(p => convert(p._1).map( (_ -> p._2) ))
  }

  def samples = raw.samples

  override def loadData(ss: Iterable[Sample]) {
    raw.loadData(ss)
  }

  override def release() {
    raw.release()
  }
}

/**
 * Utilities for converting Probe IDs.
 */
object IDConverter {
  def checkDuplicates(map: Map[ProbeId, Iterable[ProbeId]]) {
    val pairs = map.toSeq.flatMap(x => (x._2.map(y => (x._1, y))))
    val bySnd = pairs.groupBy(_._2)
    val manyToOne = bySnd.filter(_._2.size > 1)
    if (!manyToOne.isEmpty) {
      Console.err.println("Warning: The following keys have multiple incoming mappings in an ID conversion:")
      println(manyToOne.keys)
      //      throw new Exception("Invalid ID conversion map")
    }
  }

  def convert(conversion: Map[ProbeId, Iterable[ProbeId]],
              speculativeConversion: Map[ProbeId, Iterable[ProbeId]] = Map())
             (raw: ColumnExpressionData): ColumnExpressionData =
    new IDConverter(raw, conversion, speculativeConversion)

  /**
   * Create an IDConverter from an affymetrix annotation file and a specified
   * "foreign" (non-probe ID) column.
   */
  def fromAffy(file: String, column: String) = {
    import t.platform.affy._
    val conv = new t.platform.affy.IDConverter(file, Converter.columnLookup(column))
    convert(conv.foreignToAffy)(_)
  }


  def fromEnsembl(file: String) = {
    convert(EnsemblPlatformHelper.loadConversionTable(file))(_)
  }

  val probeIdColumn = "probe_id"
  val mirbaseIdColumn = "mirbase_id"

  /**
   * Create an IDConverter from a mirbase translation file with at least the two columns above.
   * @param file
   * @return
   */
  def fromMirbase(file: String, validProbes: Iterable[String]) = {
    val lookup = validProbes.toSet
    val colMap = TSVFile.readMap("", file, true)
    val expanded = (
      colMap(probeIdColumn) zip colMap(mirbaseIdColumn)
    ) . flatMap(x => {
      val src = x._1
      val dsts = x._2.split(",")
      dsts.map(d => (x._1, d))
    })

    val mapping = expanded.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))
    val validated = mapping.map(x => (x._1, x._2.filter(lookup.contains))).filter(_._2.nonEmpty)
    val speculative = mapping.map(x => (x._1, x._2.flatMap(dst => Seq(s"$dst-3p", s"$dst-5p"))))

    convert(validated, speculative)(_)
  }

  /**
   * Given a command line argument, identify the correct conversion method.
   */
  def fromArgument(param: Option[String], context: Context): (ColumnExpressionData => ColumnExpressionData) = {
    val AffyPtn = "affy:(.+):(.+)".r
    val EnsemblPtn = "ensembl:(.+)".r
    val MirbasePtn = "mirbase:(.+):(.+)".r
    param match {
      case Some(AffyPtn(file, col)) =>
        //e.g. affy:affy_annot.csv:Ensembl
        fromAffy(file, col)
      case Some(EnsemblPtn(file)) =>
        //e.g. ensembl:ensembl_mapping.tsv
        fromEnsembl(file)
      case Some(MirbasePtn(platform, file)) =>
        //e.g. mirbase:mirbase-v21:mirbase_mapping.tsv
        fromMirbase(file, context.probeStore.forPlatform(platform))
      case Some(x) =>
        throw new Exception(s"Unknown ID conversion specifier $x")
      case None => (x => x)
    }
  }

  /**
   * Given a platform ID, identify the correct conversion method.
   * @param platform
   * @param context
   * @param conversionFile
   * @return
   */
  def fromPlatform(platform: String, context: Context, conversionFile: String) = {
    Species.supportedSpecies.find(s => {
      s.ensemblPlatform == platform
    }) match {
      case Some(sp) =>
        println(s"Detected ensembl conversion into $platform")
        fromEnsembl(conversionFile)
      case None =>
        if (platform.startsWith("mirbase-v")) {
          println(s"Detected mirbase conversion into $platform")
          fromMirbase(conversionFile, context.probeStore.forPlatform(platform))
        } else {
          throw new Exception(s"Unable to convert probes into platform $platform")
        }
    }
  }
}
