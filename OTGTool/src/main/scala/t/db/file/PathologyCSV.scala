package t.db.file

import scala.io.Source
import otg.Pathology
import scala.annotation.tailrec
import scala.io.Codec

case class PathologyItem(barcode: String,
  expId: String,
  groupId: String,
  individualId: String,
  compoundName: String,
  doseLevel: String,
  exposureTime: String,
  organ: String,
  detail: Pathology)

/**
 * Open TG-GATEs pathology data CSV file.
 * May be downloaded from
 * http://dbarchive.biosciencedbc.jp/en/open-tggates/download.html
 */
class PathologyCSV(file: String) {
  def lines = Source.fromFile(file)(Codec.UTF8).getLines.toSeq

  val items = lines.drop(1).map(l => {
    val s = mergeQuoted(l.split(",").toVector)
    val finding = s(8)
    val topography = s(9)
    val grade = s(10)
    val spontaneous = s(11).toBoolean
    val p = Pathology(Some(finding), Some(topography), Some(grade),
        spontaneous, s(0))

    new PathologyItem(s(0), s(1), s(2), s(3), s(4), s(5), s(6),
      s(7), p)
  })

  /**
   * Merge strings such as "Bile duct, interlobular" back together
   * since they will have been split
   */
  @tailrec
  private def mergeQuoted(spl: Vector[String],
      soFar: Vector[String] = Vector()): Vector[String] = {
    spl match {
      case a +: b +: rest =>
        if (a.startsWith("\"") && !a.endsWith("\"")) {
          mergeEndquote(a.drop(1) + ",", b +: rest, soFar)
        } else {
          mergeQuoted(b +: rest, soFar :+ a)
        }
      case Vector(a) => soFar :+ a
      case Vector() => soFar
    }
  }

  /**
   * Look for a matching end quote
   */
  @tailrec
  private def mergeEndquote(soFar: String, spl: Vector[String],
      outerAcc: Vector[String]): Vector[String] = {
    spl match {
      case a +: rest =>
        if (a.endsWith("\"")) {
          mergeQuoted(rest, outerAcc :+ (soFar + a.dropRight(1)))
        } else {
          mergeEndquote(s"$soFar,$a", rest, outerAcc)
        }
      case Vector() =>
        throw new Exception("Parse error: unterminated quote")
    }
  }
}
