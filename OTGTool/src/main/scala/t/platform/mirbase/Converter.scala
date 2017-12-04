package t.platform.mirbase

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

  def mirnaFeatures = features.filter(_.featureType == "miRNA")

  def mirnaRecords = mirnaFeatures.flatMap(_.mirnaRecords)

}

/**
 * Reads raw miRBase dat files and produces T platform format files.
 */
object Converter {

  val speciesOfInterest = Species.values.map(_.shortCode)

  def main(args: Array[String]) {
      val lines = Source.fromFile(args(0)).getLines()
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

      val mrecs = remainingRecords.flatMap(_.mirnaRecords)
      for (mr <- mrecs) {
        println(mr.asTPlatformRecord)
      }
  }
}
