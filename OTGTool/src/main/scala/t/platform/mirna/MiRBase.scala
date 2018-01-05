package t.platform.mirna

import scala.io.Source
import t.platform.Species
import scala.language.postfixOps

case class MirnaRecord(id: String, accession: String, species: Species.Species,
  title: String) {
  def asTPlatformRecord: String = {
    s"$id\ttitle=$title,accession=$accession,species=${species.longName}"
  }
}

case class Feature(data: Seq[String]) {
  def featureType = data.head.split("\\s+")(1)

  val AccRegex = ".*accession=\"(.*)\""r
  val ProdRegex = ".*product=\"(.*)\""r

  def mirnaRecords: Iterable[MirnaRecord] =
    for (
      acc <- data.collect { case AccRegex(a) => a };
      prod <- data.collect { case ProdRegex(p) => p };
      sp <- Species.values.find(s => prod.startsWith(s.shortCode));
      mr = MirnaRecord(prod, acc, sp, prod)
    ) yield mr
}

case class RawRecord(data: Seq[String]) {
  private def featuresFrom(rem: Seq[String]): Vector[Feature] = {
    if (rem.isEmpty) {
      Vector()
    } else {
      val (cur, next) = rem.drop(1).span(_.matches("FT\\s+/.*"))
      Feature(rem.head +: cur) +: featuresFrom(next)
    }
  }

  def features: Iterable[Feature] =
    featuresFrom(data.filter(_.startsWith("FT")))

  val SeqRegex = "([actugACTUG]+)".r

  /*
   * Example:
   *
     SQ   Sequence 99 BP; 26 A; 19 C; 24 G; 0 T; 30 other;
        uacacugugg auccggugag guaguagguu guauaguuug gaauauuacc accggugaac        60
        uaugcaauuu ucuaccuuac cggagacaga acucuucga                               99
     //
   */
  def sequence: Option[String] = {
    val i = data.indexWhere(_.startsWith("SQ"))
    if (i == -1) {
      None
    } else {
      val parts = data.drop(i + 1).takeWhile(_.startsWith("  "))
      val subParts = parts.flatMap(p => p.split("\\s+").collect({ case SeqRegex(s) => s }))
      Some(subParts.mkString(""))
    }
  }

  def mirnaFeatures = features.filter(_.featureType == "miRNA")

  def mirnaRecords = mirnaFeatures.flatMap(_.mirnaRecords)

}

/**
 * Reads raw miRBase dat files and produces T platform format files.
 */
object MiRBaseConverter {

  val speciesOfInterest = Species.values.map(_.shortCode)

  def main(args: Array[String]) {
      val lines = Source.fromFile(args(0)).getLines()
      val command = args(1)

      var (record, next) = lines.span(! _.startsWith("//"))

      def remainingRecords: Stream[RawRecord] = {
        if (!lines.hasNext) {
          Stream.empty
        } else {
          val r = RawRecord(lines.takeWhile(! _.startsWith("//")).toSeq)
          lines.drop(1)
          Stream.cons(r, remainingRecords)
        }
      }

    command match {
      case "tplatform" =>
        val mrecs = remainingRecords.flatMap(_.mirnaRecords)
        for (mr <- mrecs) {
          println(mr.asTPlatformRecord)
        }
      case "sequences" =>
        for (
          rr <- remainingRecords;
          seq <- rr.sequence;
          mr <- rr.mirnaRecords
        ) {
          println(s"${mr.id}\t$seq")
        }
      case _ => throw new Exception("Unknown command. Please specify either tplatform or sequences.")
    }
  }
}
