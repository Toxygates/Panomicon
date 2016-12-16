package t.db.file

import scala.io.Source
import otg.Pathology

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
  def lines = Source.fromFile(file).getLines.toSeq

  lazy val items = lines.map(l => {
    val s = l.split("\\s+")
    val finding = s(8)
    val topography = s(9)
    val grade = s(10)
    val spontaneous = s(11).toBoolean
    val p = Pathology(Some(finding), Some(topography), Some(grade), spontaneous)

    new PathologyItem(s(0), s(1), s(2), s(3), s(4), s(5), s(6),
      s(7), p)
  })
}
