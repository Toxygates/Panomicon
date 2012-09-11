package otgviewer.server
import otgviewer.shared._
import otg.ExprValue

object KCServiceImplS {
  import scala.collection.JavaConversions._

  def barcodes(columns: Iterable[DataColumn]): Array[String] = {
    columns.flatMap(_ match {
      case g: Group   => g.getBarcodes
      case b: Barcode => Array(b)      
    }).map(_.getCode).toArray    
  }

  def barcodes4J(columns: java.lang.Iterable[DataColumn]): Array[String] = barcodes(columns)

  def computeColumn(col: DataColumn, rawData: Array[ExprValue], colMap: Map[String, Int]): ExprValue = {
    col match {
      case g: Group => {
        val bcs = g.getBarcodes
        val ids = bcs.map(b => colMap(b.getCode))
        val vs = ids.map(rawData(_))
        val call = (if (vs.exists(_.call != 'A')) { 'P' } else { 'A' })
        val pvals = vs.filter(_.call != 'A')
        new ExprValue(pvals.map(_.value).sum / pvals.size, call, rawData(0).probe)
      }
      case b: Barcode => {        
        rawData(colMap(b.getCode))
      }
      case _ => {
        throw new Exception("Unexpected column type")
      }
    }
  }
  
  private def makeColMap(orderedBarcodes: Array[String]): Map[String, Int] = Map() ++ orderedBarcodes.zipWithIndex.map(x => (x._1, x._2))
  
  def computeRow(cols: Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]], 
      orderedBarcodes: Array[String], 
      row: Int): Array[ExprValue] = {
    cols.map(computeColumn(_, rawData(row), makeColMap(orderedBarcodes))).toArray
  }
  
  def computeRow4J(cols: java.lang.Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]], 
      orderedBarcodes: Array[String],
      row: Int): Array[ExprValue] = computeRow(cols, rawData, orderedBarcodes, row)
  
 
  def computeRows(cols: Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]],
      orderedBarcodes: Array[String]): Array[Array[ExprValue]] = {
    (0 until rawData.size).par.map(r => cols.map(computeColumn(_, rawData(r), makeColMap(orderedBarcodes))).toArray).seq.toArray        
  }
  
  def computeRows4J(cols: java.lang.Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]],
      orderedBarcodes: Array[String]): Array[Array[ExprValue]] = computeRows(cols, rawData, orderedBarcodes)
  
}